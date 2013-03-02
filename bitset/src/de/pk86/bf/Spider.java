package de.pk86.bf;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

import electric.xml.Element;
import electric.xml.Elements;
/**
 * @deprecated
 * @author peter
 */
class Spider {
	// Attributes
	private ArrayList<String> extensions = new ArrayList<String>();
	private ArrayList<String> directories = new ArrayList<String>();
	private ObjectItemService sv;
	Spider (ObjectItemService sv, ArrayList<String> ext) {
		this.sv = sv;
		this.extensions = ext;
	}
	Spider(ObjectItemService sv, Element spiderEle) {
		this.sv = sv;
		Elements eles = spiderEle.getElements("FileExtension");
		while (eles.hasMoreElements()) {
			Element ele = eles.next();
			String ext = ele.getTextString();
			extensions.add(ext);
		}
		//
		eles = spiderEle.getElements("Directory");
		while (eles.hasMoreElements()) {
			Element ele = eles.next();
			String dir = ele.getTextString();
			directories.add(dir);
		}
	}
	// Methods
	void index() {
		for (int i = 0; i < directories.size(); i++) {
			String dir = (String)directories.get(i);
			this.index(dir);
		}
	}
	/**
	 * @deprecated Überarbeiten
	 * @param directory
	 */
	void index(String directory) {
		File dir = new File(directory);
		String[] files = dir.list();
		for (int i = 0; i < files.length; i++) {
			String sf = files[i];
			for (int j = 0; j < extensions.size(); j++) {
				String ext = extensions.get(j);
				if (sf.toLowerCase().endsWith(ext)) {
					try {					
						File f = new File(directory, sf);
						FileReader reader = new FileReader(f);
						//long oid = sv.createObject();
						//sv.indexObject(oid, reader, true, true);
						reader.close();
						System.out.print(".");
					} catch (Exception ex) {
						System.err.println(ex.getMessage());
					}
				}
			}
		}
	}
}
