package com.ragones.smugmug;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;

import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import java.text.ParseException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.io.IOUtils;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import com.ragones.SmugSynch;

public class SmugMug 
{
	// constants **************************************************************
	
	private static String URL_ENCODING_FORMAT = "UTF-8";

	// static objects *********************************************************
	
	private static Logger logger = Logger.getLogger(SmugMug.class.getName());
	static
	{
		// not working correctly
		logger.setLevel(Level.FINE);
	}
	
	// static vars ************************************************************

	private static String apiURL = "api.smugmug.com/hack/rest/1.2.0/?";
	private static String uploadURL = "upload.smugmug.com/";
	
	private static String apiKey = "fr1LSUEbodNUInDHN3Qf5qPNSUP9sXKA";
	private static String emailAddress = ""; // Your SmugMug username
	private static String password = "";  // Your SmugMug password

	private static String methodLogin = "smugmug.login.withPassword";
	private static String methodLogout = "smugmug.logout";
	private static String methodGetCategories = "smugmug.categories.get";
	private static String methodCreateAlbum = "smugmug.albums.create";
	private static String methodGetAlbum = "smugmug.albums.get";
	private static String methodGetImages = "smugmug.images.get";
	private static String methodGetImageExif = "smugmug.images.getEXIF";
		
	private static String argMethod = "method=";
	private static String argAPIKey = "APIKey=";
	private static String argEmailAddress = "EmailAddress=";
	private static String argPassword = "Password=";
	private static String argSessionID = "SessionID=";
	private static String argAlbumID = "AlbumID=";
	private static String argHeavy = "Heavy=";
	private static String argImageID = "ImageID=";
	private static String argTitle = "Title=";
	private static String argCategoryID = "CategoryID=";
	private static String argPublic = "Public=";
	
	private static String categoryOther = "Other";

	private static String headerUserAgent = "User-Agent";
	private static String headerContentLength = "Content-Length";
	private static String headerContentMD5 = "Content-MD5";
	private static String headerSmugSessionID = "X-Smug-SessionID";
	private static String headerSmugVersion = "X-Smug-Version";
	private static String headerSmugResponseType = "X-Smug-ResponseType";
	private static String headerSmugAlbumID = "X-Smug-AlbumID";
	private static String headerSmugImageID = "X-Smug-ImageID";
	private static String headerSmugFileName = "X-Smug-FileName";

	
	// instance vars **********************************************************
	
	private String sessionId;
	
	// interface **************************************************************
	
	public void login() throws IOException, SAXException
	{
		URL url = new URL("https://" + apiURL +
						  argMethod + URLEncoder.encode(methodLogin, URL_ENCODING_FORMAT) + "&" +
						  argAPIKey + URLEncoder.encode(apiKey, URL_ENCODING_FORMAT) + "&" +
						  argEmailAddress + URLEncoder.encode(emailAddress, URL_ENCODING_FORMAT) + "&" +
						  argPassword + URLEncoder.encode(password, URL_ENCODING_FORMAT));
		
		logger.info(url.toString());
		
		processResponse(url, new LoginContentHandler());
		logger.fine("sessionId>" + sessionId);
	}
	
	private class LoginContentHandler extends DefaultHandler
	{
		public void startElement(String uri,
								 String localName,
								 String qName,
								 Attributes attributes) throws SAXException
		{
			if("Session".equals(localName))
			{
				sessionId = attributes.getValue("id");
			}
		}
	}
	
	public void logout() throws IOException, SAXException
	{
		if(sessionId == null)
			return;
		
		URL url = new URL("http://" + apiURL +
				  argMethod + URLEncoder.encode(methodLogout, URL_ENCODING_FORMAT) + "&" +
				  argSessionID + URLEncoder.encode(sessionId, URL_ENCODING_FORMAT));
		logger.fine(url.toString());
		
		//printResponse(url);
		processResponse(url, null);
	}
	
	public String getOtherCategory() throws IOException, SAXException
	{
		if(sessionId == null)
			return null;
		
		URL url = new URL("http://" + apiURL +
				  argMethod + URLEncoder.encode(methodGetCategories, URL_ENCODING_FORMAT) + "&" +
				  argSessionID + URLEncoder.encode(sessionId, URL_ENCODING_FORMAT));
		
		CategoryContentHandler contentHandler = new CategoryContentHandler();
		
		processResponse(url, contentHandler);
		
		return contentHandler.getCategoryId();
	}
	
	private class CategoryContentHandler extends DefaultHandler
	{
		private String categoryId;
		
		public void startElement(String uri,
				 String localName,
				 String qName,
				 Attributes attributes) throws SAXException
		{
			if("Category".equals(localName) && categoryOther.equals(attributes.getValue("Title")))
			{
				categoryId = attributes.getValue("id");
			}
		}
		
		private String getCategoryId()
		{
			return categoryId;
		}
	}
	
	public String createAlbum(String title, String categoryId) throws IOException, SAXException
	{
		if(sessionId == null)
			return null;
		
		URL url = new URL("http://" + apiURL +
				  argMethod + URLEncoder.encode(methodCreateAlbum, URL_ENCODING_FORMAT) + "&" +
				  argSessionID + URLEncoder.encode(sessionId, URL_ENCODING_FORMAT) + "&" +
				  argTitle + URLEncoder.encode(title, URL_ENCODING_FORMAT) + "&" +
				  argCategoryID + URLEncoder.encode(categoryId, URL_ENCODING_FORMAT) + "&" +
				  argPublic + URLEncoder.encode("0", URL_ENCODING_FORMAT));	
		
		CreateAlbumContentHandler contentHandler = new CreateAlbumContentHandler();
		processResponse(url, contentHandler);
		logger.fine("albumId>" + contentHandler.getAlbumId());
		
		return contentHandler.getAlbumId();	
	}

	private class CreateAlbumContentHandler extends DefaultHandler
	{
		private String albumId;
		
		public void startElement(String uri,
								 String localName,
								 String qName,
								 Attributes attributes) throws SAXException
		{
			if("Album".equals(localName))
			{
				albumId = attributes.getValue("id");
			}
		}
		
		public String getAlbumId()
		{
			return albumId;
		}
	}
	
	public String getAlbum(String title) throws IOException, SAXException
	{
		if(sessionId == null)
			return null;
		
		URL url = new URL("http://" + apiURL +
				  argMethod + URLEncoder.encode(methodGetAlbum, URL_ENCODING_FORMAT) + "&" +
				  argSessionID + URLEncoder.encode(sessionId, URL_ENCODING_FORMAT));	
		
		GetAlbumContentHandler contentHandler = new GetAlbumContentHandler(title);
		processResponse(url, contentHandler);
		logger.fine("albumId>" + contentHandler.getAlbumId());
		
		return contentHandler.getAlbumId();
	}
	
	private class GetAlbumContentHandler extends DefaultHandler
	{
		private String title;
		private String albumId;
		
		private GetAlbumContentHandler(String title)
		{
			this.title = title;
		}
		
		public void startElement(String uri,
								 String localName,
								 String qName,
								 Attributes attributes) throws SAXException
		{
			if("Album".equals(localName) && title.equals(attributes.getValue("Title")))
			{
				albumId = attributes.getValue("id");
			}
		}
		
		public String getAlbumId()
		{
			return albumId;
		}
	}	
	
	public Map<String,Image> getImagesWithExif(String albumId)  throws IOException, SAXException
	{
		if(sessionId == null)
			return null;
		
		Map<String,Image> imageMap = getImages(albumId);
		Iterator<Image> i = imageMap != null ? imageMap.values().iterator() : null;
		while(i != null && i.hasNext())
		{
			Image image = (Image)i.next();
			getImageExif(image);
		}
		return imageMap;
	}
	
	public Map<String,Image> getImages(String albumId)  throws IOException, SAXException
	{
		if(sessionId == null)
			return null;
		
		URL url = new URL("http://" + apiURL +
				  argMethod + URLEncoder.encode(methodGetImages, URL_ENCODING_FORMAT) + "&" +
				  argSessionID + URLEncoder.encode(sessionId, URL_ENCODING_FORMAT) + "&" +
				  argAlbumID + URLEncoder.encode(albumId, URL_ENCODING_FORMAT) + "&" +
				  argHeavy + "1");	
		//printResponse(url);
		//return null;

		ImagesContentHandler contentHandler = new ImagesContentHandler();
		processResponse(url, contentHandler);
		
		logger.fine("# album images>" + 
					(contentHandler.getImageMap() == null ? "0" : Integer.toString(contentHandler.getImageMap().keySet().size())));
				
		return contentHandler.getImageMap();
	}
	
	private class ImagesContentHandler extends DefaultHandler
	{
		private Map<String,Image> imageMap = new HashMap<String,Image>();
		
		public void startElement(String uri,
				 String localName,
				 String qName,
				 Attributes attributes) throws SAXException
		{
			if("Image".equals(localName))
			{
				Image image = new Image();
				image.setImageId(attributes.getValue("id"));
				String fileName = attributes.getValue("FileName"); 
				image.setFileName(fileName);
				imageMap.put(fileName,image);
			}
		}		
		
		public Map<String,Image> getImageMap()
		{
			return imageMap;
		}
	}
	
	public void getImageExif(Image image) throws IOException, SAXException
	{
		if(sessionId == null)
			return;
		
		URL url = new URL("http://" + apiURL +
				  argMethod + URLEncoder.encode(methodGetImageExif, URL_ENCODING_FORMAT) + "&" +
				  argSessionID + URLEncoder.encode(sessionId, URL_ENCODING_FORMAT) + "&" +
				  argImageID + URLEncoder.encode(image.getImageId(), URL_ENCODING_FORMAT));
		ImageExifContentHandler contentHandler = new ImageExifContentHandler(image);
		processResponse(url, contentHandler);
	}
	
	private class ImageExifContentHandler extends DefaultHandler
	{
		private Image image;
		
		private ImageExifContentHandler(Image image)
		{
			this.image = image;
		}
		
		public void startElement(String uri,
				 String localName,
				 String qName,
				 Attributes attributes) throws SAXException
		{
			try
			{
				if("Image".equals(localName))
				{
					image.setDateTime(attributes.getValue("DateTime"));
					logger.fine("dateTime>" + image.getDateTimeString());
				}
			}
			catch(ParseException e)
			{
				throw new SAXException(e);
			}
		}		
		
		public Image getImage()
		{
			return image;
		}
	}
	
	public void uploadImage(String albumId, File file) throws IOException, SAXException
	{
		if(sessionId == null)
			return;
		
		logger.fine("file>" + file.getName());
		
		PutMethod putMethod = new PutMethod("http://" + uploadURL + URLEncoder.encode(file.getName(), URL_ENCODING_FORMAT));
		
		byte[] buffer = IOUtils.toByteArray(new FileInputStream(file));
		
		putMethod.setRequestHeader(headerUserAgent, SmugSynch.class.toString()); // redundant
		putMethod.setRequestHeader(headerContentLength,Long.toString(file.length()));
		putMethod.setRequestHeader(headerContentMD5,DigestUtils.md5Hex(buffer));
		putMethod.setRequestHeader(headerSmugSessionID,sessionId);
		putMethod.setRequestHeader(headerSmugVersion,"1.2.0");
		putMethod.setRequestHeader(headerSmugResponseType,"REST");
		putMethod.setRequestHeader(headerSmugAlbumID,albumId);
		putMethod.setRequestHeader(headerSmugFileName,file.getName());
		
		putMethod.setRequestEntity(new ByteArrayRequestEntity(buffer));
		
		HttpClient httpClient = new HttpClient();
		int code = httpClient.executeMethod(putMethod);
		
		logger.fine("put return code>" + code);
		logger.fine(file.getName() + " added");
		
		processResponse(putMethod.getResponseBodyAsStream(),null);
	}
	
	// private interface ******************************************************
	
	private void printResponse(URL url) throws IOException
	{
		printResponse(processURL(url));
	}
	
	private void printResponse(InputStream inputStream) throws IOException
	{
		BufferedReader reader = null;
		
		try
		{
			
			reader = new BufferedReader(new InputStreamReader(inputStream));
			String line = null;
			StringBuffer response = new StringBuffer();
			while((line = reader.readLine()) != null)
			{
				response.append(line);	
			}
			logger.info(response.toString());
		}
		finally
		{
			if(reader != null)
				reader.close();
		}		
	}
	
	private void processResponse(InputStream inputStream, ContentHandler contentHandler) throws IOException, SAXException
	{
		try
		{
			XMLReader reader = XMLReaderFactory.createXMLReader();
			reader.setContentHandler(contentHandler);
			reader.parse(new InputSource(inputStream));
		}
		finally
		{
			if(inputStream != null)
				inputStream.close();
		}		
		
	}
	
	private void processResponse(URL url, ContentHandler contentHandler) throws IOException, SAXException
	{
		processResponse(processURL(url), contentHandler);
	}
	
	private InputStream processURL(URL url) throws IOException
	{
		URLConnection urlConnection = url.openConnection();
		urlConnection.setRequestProperty(headerUserAgent, SmugSynch.class.toString());
		return urlConnection.getInputStream();
	}
} 
