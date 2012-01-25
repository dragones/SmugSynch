package com.ragones;

import com.ragones.smugmug.Image;
import com.ragones.smugmug.SmugMug;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SmugSynch {

	// static vars ************************************************************
	
	private static JPGFilenameFilter jpgFilter = new JPGFilenameFilter();
	private static DirectoryFilter directoryFilter = new DirectoryFilter();
	
	private static Logger logger = Logger.getLogger(SmugSynch.class.getName());
	static
	{
		// not working correctly
		logger.setLevel(Level.FINE);
	}
	
	private static String imageDirectory = ""; // directory to synch
	
	// methods ****************************************************************
	
	/**
	 * @param args
	 */
	public static void main(String[] args) 
	{
		try
		{
			// login to smugmug
			SmugMug smugMug = new SmugMug();
			smugMug.login();

			// get all albums
			int totalUploadCount = 0;
			File dir = new File(imageDirectory);
			if(dir.isDirectory())
			{
				File[] dirs = dir.listFiles(directoryFilter);
				for(int i = 0; i < dirs.length; ++i)
				{
					// get album
					String albumId = smugMug.getAlbum(dirs[i].getName());
					logger.fine(dirs[i].getName() + " albumId>" + albumId);

					// if album does not exist, create album
					List<File> addImages = null;
					int uploadCount = 0;
					if(albumId == null)
					{
						// get other category
						String otherCategoryId = smugMug.getOtherCategory();

						// add album
						albumId = smugMug.createAlbum(dirs[i].getName(), otherCategoryId);
						logger.fine("albumId created>" + albumId);
						
						// images to add
						addImages = Arrays.asList(dirs[i].listFiles(jpgFilter));
					}
					// else get album images
					else
					{
						Map<String,Image> imageMap = smugMug.getImages(albumId);

						// check image upload status
						addImages = getAddImages(dirs[i], imageMap);
					}
					// add images
					if(addImages != null && addImages.size() > 0)
					{
						logger.fine(addImages.size() + " images to upload to " + dirs[i].getName());
						Iterator<File> iterator = addImages.iterator();
						while(iterator.hasNext())
						{
							smugMug.uploadImage(albumId, iterator.next());
							uploadCount++;
						}
					}
					logger.info(uploadCount + " images uploaded to " + dirs[i].getName());
					totalUploadCount += uploadCount;
				}
			}
			logger.info(totalUploadCount + " images uploaded to SmugMug");	
			// logout of smugmug
			smugMug.logout();
		}
		// IOException, SAXException
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private static List<File> getAddImages(File directory, Map<String,Image> imageMap)
	{
		List<File> addImages = new ArrayList<File>();
		File[] files = directory.listFiles(jpgFilter);
		logger.fine("# files>" + files.length);
		for(int i = 0; i < files.length; ++i)
		{
			// check if uploaded
			logger.fine("file>" + files[i].getName());
			Image image = imageMap.get(files[i].getName());
			if(image == null)
			{
				logger.fine("to add>" + files[i].getName());
				addImages.add(files[i]);
			}
		}
		
		return addImages;
	}
	
	private static class DirectoryFilter implements FilenameFilter
	{
		public boolean accept(File f, String name)
		{
			File dir = new File(f,name);
			
			return dir.isDirectory();
		}		
	}
	
	private static class JPGFilenameFilter implements FilenameFilter
	{
		public boolean accept(File f, String name)
		{
			return name.endsWith("jpg") || name.endsWith("JPG");
		}
	}
}
