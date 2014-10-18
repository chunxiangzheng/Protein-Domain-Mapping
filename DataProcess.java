import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Process xml data from superfamily das server
 * @author Bruce Lab
 *
 */
public class DataProcess {
	public static void main(String[] args) {
		
	}
	public static void mapSitePercent(String in, String out, Map<String, Protein> proMap) {
		try {
			FileReader fr = new FileReader(in);
			BufferedReader br = new BufferedReader(fr);
			FileOutputStream fout = new FileOutputStream(out);
			PrintStream ps = new PrintStream(fout);
			String line = br.readLine();
			while (line != null) {
				String[] arr = line.split("\t");
				if (arr.length < 2) {
					line = br.readLine();
					continue;
				}
				String pname = arr[0];
				int site = Integer.valueOf(arr[1]);
				if (proMap.containsKey(pname)) {
					Protein p = proMap.get(pname);
					Domain domain = p.getDomainObject(site);
					if (domain != null) {
						double percent = domain.getPercent(site);
						int size = domain.getSize();
						ps.println(pname + "\t" + site + "\t" + domain.getName() + "\t" + percent + "\t" + size);
					}
				} else {
					System.out.println(pname);
				}
				line = br.readLine();				
			}
			ps.close();
			fout.close();
			br.close();
			fr.close();
		} catch (IOException e) {
			System.out.println(e.getMessage());			
		}
		
	}
	public static void mapSite(String in, String out,  Map<String, Protein> proMap) {
		try {
			FileReader fr = new FileReader(in);
			BufferedReader br = new BufferedReader(fr);
			FileOutputStream fout = new FileOutputStream(out);
			PrintStream ps = new PrintStream(fout);
			String line = br.readLine();
			while (line != null) {
				String[] arr = line.split("\t");
				if (arr.length < 2) {
					line = br.readLine();
					continue;
				}
				String pname = arr[0];
				int site = Integer.valueOf(arr[1]);
				if (proMap.containsKey(pname)) {
					Protein p = proMap.get(pname);
					String domain = p.getDomain(site);
					if (!domain.equals("N/A")) {
						ps.println(pname + "\t" + site + "\t" + domain);
					}
				} else {
					System.out.println(pname);
				}
				line = br.readLine();
			}
			ps.close();
			fout.close();
			br.close();
			fr.close();
		} catch (IOException e) {
			System.err.println(e.getMessage());
		}
	}
 	public static Map<String, Protein> buildProMap(String in) {
		Map<String, Protein> proMap = new HashMap<String, Protein>();
		try {
			FileReader fr = new FileReader(in);
			BufferedReader br = new BufferedReader(fr);
			String line = br.readLine();
			while (line != null) {
				String[] arr = line.split("\t");
				if (arr.length < 4) {
					line = br.readLine();
					continue;
				}
				String pname = arr[0];
				String dname = arr[1];
				int start = Integer.valueOf(arr[2]);
				int end = Integer.valueOf(arr[3]);
				if (proMap.containsKey(pname)) {
					Protein p = proMap.get(pname);
					Domain d = new Domain(dname, start, end);
					p.addDomain(d);
				} else {
					Protein p = new Protein(pname);
					Domain d = new Domain(dname, start, end);
					p.addDomain(d);
					proMap.put(pname, p);
				}
				line = br.readLine();
			}
			br.close();
			fr.close();
		} catch (IOException e) {
			System.err.println(e.getMessage());
		}
		return proMap;
	}
	public static void getDomain(String dir, String out) {
		try {
			FileOutputStream fout = new FileOutputStream(out);
			PrintStream ps = new PrintStream(fout);
			File fdir = new File(dir);
			File[] flist = fdir.listFiles();
			for (File f : flist) {
				String pro = f.getName();
				XMLQuickParser(dir, pro, ps);
			}
			ps.close();
			fout.close();
		} catch (IOException e) {
			
		}
	}
	public static void XMLQuickParser(String dir, String pro, PrintStream ps) {
		boolean isFeature = false;
		try {
			FileReader fr = new FileReader(dir + "/" + pro);
			BufferedReader br = new BufferedReader(fr);
			String line = br.readLine();
			String domain = "";
			int start = 0;
			int end = 0;
			while (line != null) {
				if (!isFeature && !line.trim().startsWith("<FEATURE")) {
					line = br.readLine();
					continue;
				}
				//System.out.println(line);
				line = line.trim();
				//System.out.println(line);
				if (line.startsWith("<FEATURE")) {
					int idIndex = line.indexOf("id=\"DOMAIN:");
					if (idIndex != -1) {
						isFeature = true;
						line = br.readLine();
						//System.out.println(line);
						continue;
					}
				}
				if (line.startsWith("</FEATURE")) {
					//System.out.println(line);
					
					if (isFeature) {
						//System.out.println(line);
						ps.println(pro + "\t" + domain + "\t" + start + "\t" + end);
					}
					isFeature = false;
					line = br.readLine();
					continue;
				}
				if (line.startsWith("<START")) {
					int begin = line.indexOf(">") + 1;
					int fin = line.indexOf("<", begin);
					start = Integer.valueOf(line.substring(begin, fin));
				}
				if (line.startsWith("<END")) {
					int begin = line.indexOf(">") + 1;
					int fin = line.indexOf("<", begin);
					end = Integer.valueOf(line.substring(begin, fin));
				}
				if (line.startsWith("<TYPE id=")) {
					int begin = 10;
					int fin = line.indexOf("\"", 10);
					domain = line.substring(begin, fin);
				}
				line = br.readLine();
				
			}
			br.close();
			fr.close();
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}
	public static void extractDataFromXlinkDB(String in, String prolist, String sitelist) {
		try {
			FileReader fr = new FileReader(in);
			BufferedReader br = new BufferedReader(fr);
			FileOutputStream fout = new FileOutputStream(prolist);
			PrintStream ps = new PrintStream(fout);
			FileOutputStream fout2 = new FileOutputStream(sitelist);
			PrintStream ps2 = new PrintStream(fout2);
			Map<String, Set<Integer>> siteMap = new HashMap<String, Set<Integer>>();
			//Start parse input file from xlinkdb table download
			String line = br.readLine();
			while (line != null) {
				String[] arr = line.split("\t");
				String proA = arr[2];
				String proB = arr[11];
				int siteA = Integer.valueOf(arr[1]) + Integer.valueOf(arr[5]) + 1;
				int siteB = Integer.valueOf(arr[10]) + Integer.valueOf(arr[14]) + 1;
				addSite(proA, siteA, siteMap);
				addSite(proB, siteB, siteMap);
				line = br.readLine();
			}
			//write prolsit file and sitelist file
			for (String key : siteMap.keySet()) {
				ps.println(key);
				Set<Integer> s = siteMap.get(key);
				for (int i : s) {
					ps2.println(key + "\t" + i);
				}
			}
			
			
			ps2.close();
			fout2.close();
			ps.close();
			fout.close();
			br.close();
			fr.close();
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}
	public static void addSite(String pro, int site, Map<String, Set<Integer>> siteMap) {
		if (siteMap.containsKey(pro)) {
			siteMap.get(pro).add(site);
		} else {
			Set<Integer> siteSet = new HashSet<Integer>();
			siteSet.add(site);
			siteMap.put(pro, siteSet);
		}
	}
}
class Protein {
	private String pname;
	private ArrayList<Domain> domains;
	public Protein(String s) {
		this.pname = s;
		domains = new ArrayList<Domain>();
	}
	public void addDomain(Domain d) throws IllegalArgumentException {
		if (d == null) throw new IllegalArgumentException();
		domains.add(d);
	}
	public String getName() {return pname;}
	public String getDomain(int i) {
		String domain = "N/A";
		for (Domain d : domains) {
			if (d.containSite(i)) domain = d.getName();
		}
		return domain;
	}
	public Domain getDomainObject(int i) {
		for (Domain d : domains) {
			if (d.containSite(i)) return d;
		}
		return null;
	}
	
}
class Domain {
	private String name;
	private int start, end;
	public Domain(String s, int start, int end) {
		this.name = s;
		this.start = start;
		this.end = end;
	}
	public String getName() {return name;}
	public int getStart() {return start;}
	public int getEnd() {return end;}
	public int getSize() {return end - start + 1;}
	public double getPercent(int i) {
		double percent = (double) (i - start) / (end - start);
		if (percent < 0) return -1;
		return percent;
	}
	public boolean containSite(int i) {
		return i >= start && i <= end;
	}
}
