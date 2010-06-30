/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.APIAuthenticationException;
import org.openmrs.api.context.Context;
import org.openmrs.module.Module;
import org.openmrs.module.ModuleRepository;
import org.openmrs.util.OpenmrsConstants;

/**
 * 
 */
public class FindModulesServlet extends HttpServlet {

	private static final long serialVersionUID = 1456733423L;
	
	private static Log log = LogFactory.getLog(FindModulesServlet.class);

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}
	
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		if (!Context.hasPrivilege(OpenmrsConstants.PRIV_MANAGE_MODULES)) {
			throw new APIAuthenticationException("Privilege required: " + OpenmrsConstants.PRIV_MANAGE_MODULES);
		}

		response.setContentType("text/json");
		response.setHeader("Cache-Control", "no-cache");
		PrintWriter out = response.getWriter();
		
		Integer iTotalRecords = 0;
		
		// the following parameters are described in http://www.datatables.net/usage/server-side
		final int iDisplayStart = getIntParameter(request, "iDisplayStart", 0);
		final int iDisplayLength = getIntParameter(request, "iDisplayLength", 100);
		final String sSearch = request.getParameter("sSearch");

		final int iSortingCols = getIntParameter(request, "iSortingCols", 0);
		final int[] sortingCols = new int[iSortingCols];
		final String[] sortingDirs = new String[iSortingCols];
		for (int i = 0; i < iSortingCols; i++) {
			final int iSortCol = getIntParameter(request, "iSortCol_" + i, 0);
			final String iSortDir = request.getParameter("iSortDir_" + i);
			sortingCols[i] = iSortCol;
			sortingDirs[i] = iSortDir;
		}
		final String sEcho = request.getParameter("sEcho");

		List<Module> modules;
		try {
			modules = ModuleRepository.searchModules(sSearch);
			iTotalRecords = ModuleRepository.getNoOfModules();
		}
		catch (Throwable t) {
			System.out.println("Error finding modules: " + t);
			t.printStackTrace(System.out);
			throw new ServletException(t);
		}
		
		final int iTotalDisplayRecords = modules.size();
		int fromIndex = iDisplayStart;
		int toIndex = fromIndex + iDisplayLength;
		if (fromIndex < 0) {
			fromIndex = 0;
		}
		if (toIndex > iTotalDisplayRecords) {
			toIndex = iTotalDisplayRecords;
		}
		if (fromIndex > toIndex) {
			int aux = toIndex;
			toIndex = fromIndex;
			fromIndex = aux;
		}
		modules = modules.subList(fromIndex, toIndex);

		final String jsonpcallback = request.getParameter("callback");
		
		if (jsonpcallback != null)
			out.print(jsonpcallback + "(");
		
		StringBuffer output = new StringBuffer();

		output.append("{");
		try {
			int sEchoVal = Integer.valueOf(sEcho);
			output.append("\"sEcho\":" + sEchoVal + ",");
		}
		catch (NumberFormatException nfe) {}
		output.append("\"iTotalRecords\":" + iTotalRecords + ",");
		output.append("\"iTotalDisplayRecords\":" + iTotalDisplayRecords + ",");
		output.append("\"sColumns\": \"Action,Name,Version,Author,Description\",");
		output.append("\"aaData\":");
		output.append("[");
		boolean first = true;
		for (Module module : modules) {
			if (first) {
				first = false;
			} else {
				output.append(",");
			}
			output.append("[");
			output.append("\"" + module.getDownloadURL() + "\",");
			output.append("\"" + module.getName() + "\",");
			output.append("\"" + module.getVersion() + "\",");
			output.append("\"" + module.getAuthor() + "\",");
			output.append("\"" + escape(module.getDescription()) + "\"");
			output.append("]");
		}
		output.append("]");
		output.append("}");
		
		// to support cross site scripting and jquery's jsonp
		if (jsonpcallback != null)
			output.append(")");
		
		log.debug("Sending JSON output to datatable");

		out.print(output.toString());
		
	}

	private int getIntParameter(HttpServletRequest request, String param, int defaultVal) {
		try {
			return Integer.valueOf(request.getParameter(param));
		}
		catch (NumberFormatException nfe) {
			return defaultVal;
		}
	}
	
	/**
	 * copied from http://json-simple.googlecode.com/svn/trunk/src/org/json/simple/JSONValue.java
	 * Revision 184 Escape quotes, \, /, \r, \n, \b, \f, \t and other control characters (U+0000
	 * through U+001F).
	 * 
	 * @param s
	 * @return
	 */
	private String escape(String s) {
		if (s == null)
			return null;
		StringBuffer sb = new StringBuffer();
		escape(s, sb);
		return sb.toString();
	}
	
	/**
	 * @param s - Must not be null.
	 * @param sb
	 */
	private void escape(String s, StringBuffer sb) {
		for (int i = 0; i < s.length(); i++) {
			char ch = s.charAt(i);
			switch (ch) {
				case '"':
					sb.append("\\\"");
					break;
				case '\\':
					sb.append("\\\\");
					break;
				case '\b':
					sb.append("\\b");
					break;
				case '\f':
					sb.append("\\f");
					break;
				case '\n':
					sb.append("\\n");
					break;
				case '\r':
					sb.append("\\r");
					break;
				case '\t':
					sb.append("\\t");
					break;
				case '/':
					sb.append("\\/");
					break;
				default:
					// Reference: http://www.unicode.org/versions/Unicode5.1.0/
					if ((ch >= '\u0000' && ch <= '\u001F') || (ch >= '\u007F' && ch <= '\u009F')
					        || (ch >= '\u2000' && ch <= '\u20FF')) {
						String ss = Integer.toHexString(ch);
						sb.append("\\u");
						for (int k = 0; k < 4 - ss.length(); k++) {
							sb.append('0');
						}
						sb.append(ss.toUpperCase());
					} else {
						sb.append(ch);
					}
			}
		}// for
	}
}
