/*
 * Copyright 2012-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package hello;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.dom.DOMDocument;
import org.dom4j.dom.DOMDocumentFactory;
import org.dom4j.dom.DOMElement;
import org.dom4j.io.DOMReader;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.w3c.tidy.Tidy;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import hello.service.HelloWorldService;


@SpringBootApplication
public class SampleSimpleApplication implements CommandLineRunner {
	private static final Logger logger = LoggerFactory.getLogger(SampleSimpleApplication.class);
	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

	@Autowired
	private HelloWorldService helloWorldService;

	@Value("${year:1}")
	private int year;
	int filesCount;
	private	int fileIdx = 0;
	//develop
	private static String basicDir ="/home/roman/jura/";
	private static String fileSeparator = "/";
	DOMReader domReader = new DOMReader();
	DateTime startMillis;

	private String workDir() {
		return basicDir + "workshop-manuals"
				+ year
				+ "-"
				+ year
				+ "/";
	}
	String domain = "http://workshop-manuals.com";
	private String workDir, dirJsonName, dirLargeHtmlName;
	
	Path dirJsonStart;
	private int debugSkip;

	@Override
	public void run(String... args) {
		System.out.println(this.helloWorldService.getHelloMessage());
		System.out.println(year);
		workDir = workDir();
		dirJsonName = workDir + "OUT1json/";
		dirLargeHtmlName = workDir+ "OUT1html/";
		dirJsonStart = Paths.get(dirJsonName);
		
		
		domReader.setDocumentFactory(new DOMDocumentFactory());
		startMillis = new DateTime();
		System.out.println("The time is now " + dateFormat.format(startMillis.toDate()));
		logger.debug(dirJsonStart.toString());
		filesCount = countFiles2(dirJsonStart.toFile());
		logger.debug("Files count " + filesCount + ". The time is now " + dateFormat.format(new Date()));
		logger.debug(dirJsonName);
		try {
			makeLargeHTML();
			//makePdfFromHTML();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	private void makeLargeHTML() throws IOException {
		logger.debug("Start folder : "+dirJsonStart);
		Files.walkFileTree(dirJsonStart, new SimpleFileVisitor<Path>() {

			@Override
			public FileVisitResult visitFile(Path file,
					BasicFileAttributes attrs) throws IOException {
				final FileVisitResult visitFile = super.visitFile(file, attrs);
				fileIdx++;
				
				logger.debug(fileIdx + "" + fileSeparator + filesCount +  procentWorkTime() + file);
				logger.debug(fileSeparator);
				String fileStr = file.toFile().toString();
				logger.debug(fileStr);
				if(fileSeparator.equals("\\"))
					fileStr = fileStr.replace(fileSeparator, "/");
				String[] split = fileStr.split("/");
				String manufacturerName = split[split.length-2];
				Map<String, Object> jsonMap = readJsonDbFile2map(fileStr);
				String autoName = (String) jsonMap.get("autoName");
				String autoNameWithManufacturer = split[split.length-1].replace(".json", "");
//				String autoNameWithManufacturer = manufacturerName+"_-_"+autoName;
				String pdfTitleAutoNameWithManufacturer = manufacturerName+ " :: "+autoName;
				logger.debug(autoNameWithManufacturer +" -- BEGIN");
				
				String htmlOutFileName2 = dirLargeHtmlName+autoNameWithManufacturer;
				logger.debug(htmlOutFileName2);
				File f = new File(htmlOutFileName2);
				if(f.exists())
				{
					logger.debug("f.exists() --  "+htmlOutFileName2);
					return visitFile;
				}
				Element autoDocBody = createAutoDocument(pdfTitleAutoNameWithManufacturer);
				autoTileNr = 0;
				bookmarkId = 0;
				debugSkip = 0;
				Element headEl = (Element) autoDocument.selectSingleNode("/html/head");
				bookmarks = headEl.addElement("bookmarks");
//				addGroupAndRealInfo2(2,getIndexList(jsonMap));
				//buildBookmark(autoDocument);
				
				addGroupAndRealInfo(bookmarks, getIndexList(jsonMap));
				logger.debug(htmlOutFileName2 +" -- GOTO SAVE");
				
				try{
//					String htmlOutFileName = dirLargeHtmlName+autoNameWithManufacturer+".html";
					saveHtml(autoDocument, htmlOutFileName2);
				}catch(Exception e){
					e.printStackTrace();
				}
				logger.debug(autoNameWithManufacturer +" -- END");
				return visitFile;
			}
			private void saveHtml(Document document, String htmlOutFileName) {
				writeToHtmlFile(document, htmlOutFileName);
			}
			private void writeToHtmlFile(Document document, String htmlOutFileName) {
				try {
					FileOutputStream fileOutputStream = new FileOutputStream(htmlOutFileName);
					//					HTMLWriter xmlWriter = new HTMLWriter(fileOutputStream, prettyPrintFormat);
					XMLWriter xmlWriter = new XMLWriter(fileOutputStream, prettyPrintFormat);
					xmlWriter.write(document);
					xmlWriter.close();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			OutputFormat prettyPrintFormat = OutputFormat.createPrettyPrint();
			private List<Map<String, Object>> getIndexList(Map<String, Object> jsonMap) {
				return (List<Map<String, Object>>) jsonMap.get("indexList");
			}

			private void addGroupAndRealInfo(Element bookmarkParent, List<Map<String, Object>> indexList) {
				if(indexList != null){
					for (Map<String, Object> map : indexList) {
						String text = (String) map.get("text");
						Element bookmarkElement = bookmarkParent.addElement("bookmark");
						bookmarkElement.addAttribute("name", text);
						bookmarkElement.addAttribute("href", "#b_"+bookmarkId++);
						String url = (String) map.get("url");
						if(url != null){
							addRealInfo(url);
						}
//						if(debugSkip > 13)
//							break;
						addGroupAndRealInfo(bookmarkElement, getIndexList(map));
					}
				}
			}
		});

	}
	private int bookmarkId;
	private Element bookmarks;
	int autoTileNr;
	private void addRealInfo(String autoTileHref) {
		autoTileNr++;
		Document domFromStream = getDomFromStream(autoTileHref);
//		addAutoTile(autoTileNr, "t1", domFromStream);
		addAutoTile(domFromStream);
	}
	private void changeImgUrl(Element autoTileElement) {
		List<Element> selectNodes = autoTileElement.selectNodes(".//img");
		for (Element bagroundImage : selectNodes) {
			Attribute srcImg = bagroundImage.attribute("src");
			if(!srcImg.getValue().contains(domain))
			srcImg.setValue(domain+"/"+srcImg.getValue());
		}
//		Element bagroundImage = (Element) autoTileElement.selectSingleNode("img[1]");
	}
	private void addBreadcrumbBefore(DOMElement autoTileElement) {
		DOMDocument document = (DOMDocument) autoTileElement.getDocument();
		DOMElement breadcrumbEl = (DOMElement) document.createElement("div");
//		breadcrumbEl.addAttribute("style", "font-weight: bold; left: 0px; position: absolute; top: 3px; font-size:x-small;");
		breadcrumbEl.addAttribute("style", "left: 0px; position: absolute; top: 3px; font-size:x-small;");
		Element lastChildElement = (Element) bookmarks.selectSingleNode("*[last()]");
		addBreadcrumbItem(lastChildElement, breadcrumbEl);
		DOMElement h4El = (DOMElement) autoTileElement.selectSingleNode("*");//.detach();
		autoTileElement.insertBefore(breadcrumbEl, h4El);
	}
	private void addBreadcrumbItem(Element bookmarkElement, Element h3El) {
		List<Element> selectChildNodes = bookmarkElement.selectNodes("*");
		if(selectChildNodes.size() > 0){
			Element lastElement = selectChildNodes.get(selectChildNodes.size() - 1);
			if(bookmarkElement.attribute("hasLink") == null)
			{
				addInnerAnchor(h3El, bookmarkElement);
			}else{
				h3El.addElement("span").addText(" > " + bookmarkElement.attributeValue("name"));
			}
			addBreadcrumbItem(lastElement, h3El);
		}else{//last in bookmark tree (null child)
			addInnerAnchor(h3El, bookmarkElement);
		}
	}
	private void addInnerAnchor(Element h3El, Element bookmarkElement) {
		String text = bookmarkElement.attributeValue("name");
		String bookmarkId = bookmarkElement.attribute("href").getValue().substring(1);
		h3El.addElement("a")
		.addAttribute("name", bookmarkId)
		.addText(" > " + text);
		bookmarkElement.addAttribute("hasLink", "1");
	}
	private void cleanSymbols(Element autoTileElement) {
		List<Element> selectNodes = autoTileElement.selectNodes(".//*[text()]");
		for (Element element : selectNodes) {
			String replace = element.getText().replace("‘", "'").replace("’", "'");
			element.setText(replace);
		}
	}
	private void addAutoTile(Document domFromStream) {
		DOMElement autoTileElement1 = (DOMElement) domFromStream.selectSingleNode("/html/body//div[@id='page1-div']");
		Element autoTileElement = autoTileElement1;
		if(autoTileElement != null){
			autoTileElement.attribute("id").setValue("auto_tile_"+autoTileNr);
			changeImgUrl(autoTileElement);
			addBreadcrumbBefore(autoTileElement1);
			cleanSymbols(autoTileElement);
			
			Element detach = (Element) autoTileElement.detach();
			autoDocBody.add(detach);
		}else{
			//audi
			Element autoTileElementFromStream = (Element) domFromStream.selectSingleNode(
					"/html/body/div/table//td[div/h2]");
			changeImgUrl(autoTileElementFromStream);
			/*
			Element autoTileNameElement = (Element) autoTileElement2.selectSingleNode("div/h2");
			autoTileNameElement.setText(autoTileName);
			 * */
			
			/*
			 * */
			List<Element> breadcrumOld = autoTileElementFromStream.selectNodes("div/h3");
			for (Element element : breadcrumOld) {
				element.detach();
			}
			autoTileElement = autoDocBody.addElement("div");
			autoTileElement.addAttribute("id","auto_tile_"+autoTileNr);
			addBreadcrumb(autoTileElement);

			cleanSymbols(autoTileElementFromStream);
			for (Iterator iterator = autoTileElementFromStream.elementIterator(); iterator.hasNext();) {
				Element element = (Element) iterator.next();
				autoTileElement.add(element.detach());
			}

		}
		/* neccesary
		 * */
	}
	private void addBreadcrumb(Element autoTileElement) {
		Element breadcrumbEl = autoTileElement.addElement("div");
//		breadcrumbEl.addAttribute("style", "font-weight: bold; left: 0px; position: absolute; top: 3px; font-size:x-small;");
		breadcrumbEl.addAttribute("style", "font-size:x-small;");
		Element lastChildElement = (Element) bookmarks.selectSingleNode("*[last()]");
		addBreadcrumbItem(lastChildElement, breadcrumbEl);
	}
	private HttpURLConnection getUrlConnection(String url) {
		try {
			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();
			con.setRequestMethod("GET");
			con.setDoOutput(true);
			con.setRequestProperty("Content-Type", "text/html"); 
			con.setRequestProperty("charset", "utf-8");
			return con;
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (ProtocolException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	private DOMDocument getDomFromStream(String url) {
		HttpURLConnection urlConnection = getUrlConnection(url);
		try {
			return getDomFromStream(urlConnection);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	private DOMDocument getDomFromStream(HttpURLConnection urlConnection) throws IOException {
		InputStream requestBody = urlConnection.getInputStream();
		
		BufferedReader in = new BufferedReader(new InputStreamReader(requestBody));
		String line = null;
		StringBuilder responseData = new StringBuilder();
		while((line = in.readLine()) != null) {
			if(line.contains("font size>")){
				line = line.replace("font size>", "font>");
			}
			if(line.contains("<g:plusone size=\"small\" annotation=\"none\"></g:plusone>")){
				line = line.replace("<g:plusone size=\"small\" annotation=\"none\"></g:plusone>", "");
			}
			responseData.append(line);
		}
		InputStream byteArrayInputStream = new ByteArrayInputStream(responseData.toString().getBytes(StandardCharsets.UTF_8));
		
		org.w3c.dom.Document html2xhtml = tidy.parseDOM(byteArrayInputStream, null);
//		org.w3c.dom.Document html2xhtml = tidy.parseDOM(requestBody, null);
		DOMDocument document = (DOMDocument) domReader.read(html2xhtml);
		return document;
	}
	Tidy tidy = getTidy();
	private Tidy getTidy() {
		Tidy tidy = new Tidy();
		tidy.setShowWarnings(false);
		tidy.setXmlTags(false);
		tidy.setInputEncoding("UTF-8");
		tidy.setOutputEncoding("UTF-8");
		tidy.setXHTML(true);// 
		tidy.setMakeClean(true);
		tidy.setQuoteNbsp(false);
		return tidy;
	}
	private Map<String, Object> readJsonDbFile2map(String fileNameJsonDb) {
		logger.debug(fileNameJsonDb);
		File file = new File(fileNameJsonDb);
		logger.debug(" o - "+file);
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> readJsonDbFile2map = null;// = new HashMap<String, Object>();
		try {
			readJsonDbFile2map = mapper.readValue(file, Map.class);
			//			logger.debug(" o - "+readJsonDbFile2map);
		} catch (JsonParseException e1) {
			e1.printStackTrace();
		} catch (JsonMappingException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		//		logger.debug(" o - "+readJsonDbFile2map);
		return readJsonDbFile2map;
	}
	public static int countFiles2(File directory) {
		int count = 0;
		System.out.println(directory);
		for (File file : directory.listFiles()) {
			if (file.isDirectory()) {
				count += countFiles2(file); 
			}else
				count++;
		}
		return count;
	}
	Document autoDocument;
	Element autoDocBody;
	Document createAutoDocument() {
		Document autoDocument = DocumentHelper.createDocument();
		Element htmElAutoDocument = autoDocument.addElement("html");
		Element headElAddElement = htmElAutoDocument.addElement("head");
		addUtf8(headElAddElement);
		htmElAutoDocument.addElement("body");
		return autoDocument;
	}
	private void addUtf8(Element headEl) {
		headEl.addElement("meta").addAttribute("charset", "utf-8");
	}
	private Element createAutoDocument(String autoName) {
		autoDocument = createAutoDocument();
		autoDocBody = (Element) autoDocument.selectSingleNode("/html/body");
		autoDocBody.addElement("h1").addText(autoName);
		return autoDocBody;
	}
	static PeriodFormatter hmsFormatter = new PeriodFormatterBuilder()
			.appendHours().appendSuffix("h ")
			.appendMinutes().appendSuffix("m ")
			.appendSeconds().appendSuffix("s ")
			.toFormatter();
	String procentWorkTime() {
		int procent = fileIdx*100/filesCount;
		String workTime = hmsFormatter.print(new Period(startMillis, new DateTime()));
		String procentSecond = " - html2pdf3 - (" + procent + "%, " + workTime + "s)";
		return procentSecond;
	}
	public static void main(String[] args) throws Exception {
		SpringApplication application = new SpringApplication(SampleSimpleApplication.class);
		application.setApplicationContextClass(AnnotationConfigApplicationContext.class);
		SpringApplication.run(SampleSimpleApplication.class, args);
	}

}
