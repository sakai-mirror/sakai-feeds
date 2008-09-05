package org.sakaiproject.feeds.impl;

import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;


public class SakaiXmlInputFilter extends FilterReader {

	protected SakaiXmlInputFilter(Reader in) {
		super(in);
	}

	public int read() throws IOException {
		int c = in.read();
		if(Character.isIdentifierIgnorable(c) 
			|| (c >= 0x00 && c <= 0x0F && c != 0x09 && c!= 0x10 && c!= 0x13)) {
			System.out.println("Character "+c+" is ignorable. Returning: "+Character.getNumericValue(c));
			return read();
		}
		return c;
	}

	public int read(char cbuf[], int off, int len) throws IOException {
		int charsRead = 0;
		int c = -1;
		while ((c = read()) > -1 && charsRead < len){
			cbuf[charsRead++] = (char) c;
		}
		return c > -1 ? charsRead : -1;
	}

}
