package com.insweat.hssd.lib.persistence.xml

import org.xml.sax.InputSource
import scala.xml.parsing.NoBindingFactoryAdapter
import scala.xml.{TopScope, Elem}
import javax.xml.parsers.{SAXParserFactory, SAXParser}
import javax.xml.validation.Schema

class SchemaAwareFactoryAdapter(schema: Schema)
        extends NoBindingFactoryAdapter {
    override def loadXML(source: InputSource, parser: SAXParser): Elem = {
        val reader = parser.getXMLReader()
        val handler = schema.newValidatorHandler()
        handler.setContentHandler(this)
        reader.setContentHandler(handler)

        scopeStack.push(TopScope)
        try{
            reader.parse(source)
        }
        finally {
            scopeStack.pop
        }
        rootElem.asInstanceOf[Elem]
    }

    override def parser: SAXParser = {
        val factory = SAXParserFactory.newInstance()
        factory.setNamespaceAware(true)
        factory.setFeature(
                "http://xml.org/sax/features/namespace-prefixes", true)
        factory.newSAXParser()
    }
}
