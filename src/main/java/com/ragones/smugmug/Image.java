package com.ragones.smugmug;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Image 
{
	// static vars ************************************************************
	
	private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	// instance vars **********************************************************
	
	private String imageId;
	private String fileName;
	private Date dateTime;

	// methods ****************************************************************
	
	public String getImageId() {
		return imageId;
	}
	public void setImageId(String imageId) {
		this.imageId = imageId;
	}
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public Date getDateTime() {
		return dateTime;
	}
	public String getDateTimeString()
	{
		return dateFormat == null ? null : dateFormat.format(dateTime);
	}
	public void setDateTime(Date dateTime) {
		this.dateTime = dateTime;
	}
	public void setDateTime(String dateTimeString) throws ParseException {
		this.dateTime = dateFormat.parse(dateTimeString);
	}

}
