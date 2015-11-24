package com.insweat.hssd.export;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.insweat.hssd.lib.essence.Database;
import com.insweat.hssd.lib.essence.EntryData;
import com.insweat.hssd.lib.essence.ValueData;
import com.insweat.hssd.lib.interop.EssenceHelper;
import com.insweat.hssd.lib.interop.Interop;
import com.insweat.hssd.lib.interop.Logging;
import com.insweat.hssd.lib.tree.EntryTree;
import com.insweat.hssd.lib.util.Subprocess;
import com.insweat.hssd.lib.util.logging.Logger;

import scala.Option;
import scala.Tuple2;

public class Exporter {
    private final String EXEC_NAME = "exec_code_gen";

    public final Logger log = Logging.getChild(
            Logging.getRoot(),
            "Exporter",
            Logging.LEVEL_MIN,
            true);

    private void verifyParentLocation(File parentLocation) {
        if(!parentLocation.exists()) {
            String err = "parentLocation %s does not exist.";
		    err = String.format(err, parentLocation);
		    throw new IllegalArgumentException(err);
        }

		if(parentLocation.isFile()) {
		    String err = "parentLocation %s is a file.";
		    err = String.format(err, parentLocation);
		    throw new IllegalArgumentException(err);
		}
    }

	public void exportDB(Database db, File parentLocation) {
	    verifyParentLocation(parentLocation);
	    
        Option<File> execCodeGen = Subprocess.findExecutable(
                parentLocation, EXEC_NAME);
        
        if(!execCodeGen.isDefined()) {
            String err = "The %s[.*] is missing or not executable.";
            throw new RuntimeException(String.format(err, EXEC_NAME));
        }

        Subprocess sp = null;
        try {
            sp = new Subprocess(
                Runtime.getRuntime().exec(new String[]{
                  execCodeGen.get().getAbsolutePath()
                })
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        
        Tuple2<String, String> rv = sp.communicate(Interop.none());
        
        if(0 != sp.proc().exitValue()) {
            throw new RuntimeException(String.format(
                    "%s: %s", EXEC_NAME, rv._2()));
        }
	    
	    exportDBData(db, parentLocation);
	}

	public void exportDBData(Database db, File parentLocation) {
	    verifyParentLocation(parentLocation);

	    EntryTree et = db.entries();
	    Map<Integer, List<ValueData>> values = new HashMap<>();
	    boolean[] containsError = new boolean[]{false};
        EssenceHelper.foreach(et, en -> {
            if(!en.isLeaf()) {
                return;
            }
            
            List<ValueData> vds = new ArrayList<>();
            EntryData ed = EntryData.of(en);
            EssenceHelper.foreach(ed, vn -> {
                ValueData vd = ValueData.of(vn);
                if(vd.value().isError()) {
                    String err = "Value contains error %s:%s.";
                    Logging.errorf(log, err, en, vd.path());
                    containsError[0] = true;
                }
                vds.add(vd);
            });
            
            values.put(Long.valueOf(ed.entryID()).intValue(), vds);
        });
        
        if(containsError[0]) {
            throw new RuntimeException("Export failed due to value errors.");
        }
        
        JsonObject entries = new JsonObject();
        for(Map.Entry<Integer, List<ValueData>> e: values.entrySet()) {
            JsonObject entry = new JsonObject();
            entries.add(String.valueOf(e.getKey()), entry);
            
            for(ValueData vd: e.getValue()) {
                entry.add(vd.path().toString(), toJson(vd.value().value()));
            }
        }
        
        Gson gson = new Gson();
        
        File folderExport = new File(parentLocation, "export");
        folderExport.mkdirs();

        File output = new File(folderExport, "entries.json");
        
        FileWriter fw = null;
        try {
            fw = new FileWriter(output);
            fw.write(gson.toJson(entries));
        } catch (IOException ex) {
            Logging.exceptionf(log, Logging.LEVEL_ERROR, ex,
                    "An error occurred while writing file %s", output);
        }
        finally {
            if(fw != null) {
                try {
                    fw.close();
                }
                catch (IOException e) {
                    Logging.exceptionf(log, Logging.LEVEL_ERROR, e,
                            "An error occurred while closing file %s", output);
                }
            }
        }
	}
	
	private JsonElement toJson(Object obj) {
	    if(obj == null) {
	        return null;
	    }

	    if(obj instanceof Boolean) {
	        return new JsonPrimitive((Boolean)obj);
	    }
	    else if(obj instanceof Number) {
	        return new JsonPrimitive((Number)obj);
	    }
	    else {
	        return new JsonPrimitive(String.valueOf(obj));
	    }
	}
	
}
