package com.esri.geoevent.datastore;

import java.io.StringReader;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.junit.Test;

public class CSVParsingTest
{

	@Test
	public void testSimpleCSVParse() throws Exception
	{
		StringReader csvServers = new StringReader("mingz14,getest1w");
		Iterable<CSVRecord> records = CSVFormat.DEFAULT.parse(csvServers);
		for (CSVRecord record : records)
		{
			int size = record.size();
			for( int i=0; i< size; i++)
			{
				System.out.println(record.get(i));
			}
		}
	}
}
