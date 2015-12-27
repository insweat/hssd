package com.insweat.hssd.export;

import java.io.File;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.insweat.hssd.lib.essence.Database;
import com.insweat.hssd.lib.essence.EntryData;
import com.insweat.hssd.lib.essence.ValExpr;
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
    private final String EXEC_NAME_CODE_GEN = "exec_code_gen";
    private final String EXEC_NAME_DATA_EXPORT = "exec_data_export";

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
                parentLocation, EXEC_NAME_CODE_GEN);
        
        if(!execCodeGen.isDefined()) {
            String err = "The %s[.*] is missing or not executable.";
            throw new RuntimeException(String.format(err, EXEC_NAME_CODE_GEN));
        }

        Subprocess sp = Subprocess.create(new String[]{
                execCodeGen.get().getAbsolutePath()});
        
        try {
            Tuple2<String, String> rv = sp.communicate(Interop.none());
            
            if(0 != sp.proc().exitValue()) {
                throw new RuntimeException(String.format(
                        "%s: %s", EXEC_NAME_CODE_GEN, rv._2()));
            }
            
            exportDBData(db, parentLocation);
        } finally {
            if(sp.proc().isAlive()) {
                sp.proc().destroyForcibly();
            }
        }
	}

	public void exportDBData(Database db, File parentLocation) {
	    verifyParentLocation(parentLocation);

        Option<File> execDataExport = Subprocess.findExecutable(
                parentLocation, EXEC_NAME_DATA_EXPORT);
        
        if(!execDataExport.isDefined()) {
            String err = "The %s[.*] is missing or not executable.";
            throw new RuntimeException(String.format(err, EXEC_NAME_DATA_EXPORT));
        }

        Subprocess sp = Subprocess.create(new String[]{
                execDataExport.get().getAbsolutePath()});

        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            StringBuilder input = new StringBuilder();

            EntryTree et = db.entries();
            boolean[] containsError = new boolean[]{false};
            EssenceHelper.foreach(et, en -> {
                if(!en.isLeaf()) {
                    return;
                }
                
                JsonObject entry = new JsonObject();
                JsonObject values = new JsonObject();

                EntryData ed = EntryData.of(en);
                entry.add("id", toJson(ed.entryID()));
                entry.add("name", toJson(en.name()));
                entry.add("values", values);

                EssenceHelper.foreach(ed, vn -> {
                    ValueData vd = ValueData.of(vn);
                    ValExpr ve = vd.value();
                    if(ve.isError()) {
                        String err = "%s::%s: %s";
                        Logging.errorf(log, err, en, vd.path(), ve.value());
                        containsError[0] = true;
                    }

                    values.add(vd.path().toString(), toJson(ve.value()));
                });
                
                // We arrange the input as a series of JSON objects, one for
                // each entry, rather than one huge JSON object for all entries,
                // so that the exporter script can handle entries incrementally.
                input.append(gson.toJson(entry))
                    .append(System.lineSeparator())
                    .append(System.lineSeparator());
            });
            
            if(containsError[0]) {
                throw new RuntimeException("Export failed due to value errors.");
            }
            
            Tuple2<String, String> rv = sp.communicate(
                    Interop.opt(input.toString()));
            
            if(0 != sp.proc().exitValue()) {
                throw new RuntimeException(String.format(
                        "%s: %s", EXEC_NAME_DATA_EXPORT, rv._2()));
            }
        } finally {
            if(sp.proc().isAlive()) {
                sp.proc().destroyForcibly();
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
