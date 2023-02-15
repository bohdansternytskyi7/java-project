package zad1;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Controller {
	private Object model;
	private int dataSize;
	private int start;
	private Bindings bindings;
	private LinkedHashMap<String, Object> sortedVars;
	
	public Controller(String modelName) {
		this.dataSize = 0;
		this.bindings = new SimpleBindings();
		this.sortedVars = new LinkedHashMap<String, Object>();
		try {
			Class<?> c = Class.forName("zad1.models." + modelName);
			Constructor<?> con = c.getConstructor();
			this.model = con.newInstance(new Object[] {});
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | NoSuchMethodException
				| SecurityException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
		} 
	}
	
	Controller readDataFrom(String fname) {
		try {
			Scanner scanner = new Scanner(new File(fname));
			while(scanner.hasNextLine()) {
				String line = scanner.nextLine();
				String[] lineData = line.split("\\s+");
				Field field;
				try {
					 if(lineData[0].equals("LATA")) {
						 this.dataSize = lineData.length - 1;
						 this.start = Integer.parseInt(lineData[1]);
						 field = this.model.getClass().getDeclaredField("LL");
						 field.setAccessible(true);
						 field.set(this.model, dataSize);
						 this.sortedVars.put("LL", dataSize);
					 } else {
						 field = this.model.getClass().getDeclaredField(lineData[0]);
						 if(!field.isAnnotationPresent(zad1.models.Bind.class))
							 continue;
						 field.setAccessible(true);
						 double[] arr = (double[]) Array.newInstance(
						            double.class, dataSize);
						 field.set(this.model, arr);
						 double lastValue = 0.0;
						 for(int i = 0; i < dataSize; i++) {
							 if(i >= lineData.length - 1) {								 
								 Array.setDouble(arr, i, lastValue);
							 }
							 else {
								 Array.setDouble(arr, i, Double.parseDouble(lineData[i + 1]));
								 lastValue = Double.parseDouble(lineData[i + 1]);
							 }
						 }
						 this.sortedVars.put(field.getName(), arr);
					 }
				} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
					e.printStackTrace();
				}
			}	
			scanner.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return this;
	}
	
	Controller runModel() {
		try {
			Method m = this.model.getClass().getDeclaredMethod("run");
			m.setAccessible(true);
			m.invoke(this.model);
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | InvocationTargetException e) {
			e.printStackTrace();
		}
		
		Field[] fields = this.model.getClass().getDeclaredFields();
		for(Field f: fields) {
			if(f.isAnnotationPresent(zad1.models.Bind.class)) {
				f.setAccessible(true);
				try {
					this.bindings.put(f.getName(), f.get(this.model));
					if(this.sortedVars.get(f.getName()) == null)
						this.sortedVars.put(f.getName(), f.get(this.model));
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}					 
			 }
		}
		return this;
	}
	
	Controller runScriptFromFile(String fname) {		
		ScriptEngineManager mgr = new ScriptEngineManager();
		ScriptEngine engine = mgr.getEngineByName("groovy");
		
		try {
			engine.eval(new FileReader(fname), this.bindings);
		} catch ( IllegalArgumentException | ScriptException | FileNotFoundException e) {
			e.printStackTrace();
		}
		
		return this;
	}
	
	Controller runScript(String script) {
		ScriptEngineManager mgr = new ScriptEngineManager();
		ScriptEngine engine = mgr.getEngineByName("groovy");
		try {
			engine.eval(script, this.bindings);
		} catch ( IllegalArgumentException | ScriptException e) {
			e.printStackTrace();
		}
		return this;
	}
	
	String getResultsAsTsv() {
		String result = "";
		Set<String> keys = sortedVars.keySet();
		ArrayList<String> toDelete = new ArrayList<>();
		for(Entry<String, Object> entry : bindings.entrySet()) {
			if(entry.getKey().length() <= 1) {
				toDelete.add(entry.getKey());
				continue;
			}
			if(sortedVars.get(entry.getKey()) == null)
				sortedVars.put(entry.getKey(), entry.getValue());
		}
		for(String s: toDelete)
			bindings.remove(s);
		for(String key: keys) {
			result += key.equals("LL") ? "LATA\t" : key + "\t";
			for(int i = 0; i < this.dataSize; i++) {
				if(key.equals("LL"))
					result += (this.start + i) + "\t";
				else {
					double[] arr = (double []) sortedVars.get(key);
					result +=  arr[i] + "\t";
				}
			}
			result += "\n";
		}
		return result;
	}
}