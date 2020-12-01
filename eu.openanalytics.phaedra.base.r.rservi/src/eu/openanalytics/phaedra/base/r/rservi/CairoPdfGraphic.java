/*******************************************************************************
 * Copyright (c) 2009-2010 WalWare/RJ-Project (www.walware.de/goto/opensource).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package eu.openanalytics.phaedra.base.r.rservi;

import java.io.OutputStream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.statet.rj.services.FunctionCall;
import org.eclipse.statet.rj.services.RService;
import org.eclipse.statet.rj.services.util.Graphic;


public class CairoPdfGraphic extends Graphic {
	
	private double width, height;
	private String unit;
		
	@Override
	public void setSize(final double width, final double height, final String unit) {
		if (UNIT_PX.equals(unit)) {
			throw new IllegalArgumentException("Pixel not supported by PDF graphic.");
		}
		this.width = width;
		this.height = height;
		this.unit = unit;
	}
	
	public byte[] create(final FunctionCall plot, final RService service, final IProgressMonitor monitor) throws CoreException {
		final String filename = "plot-"+System.nanoTime()+".pdf";
		prepare(filename, service, monitor);
		try {
			plot.evalVoid(monitor);
		} finally {
			service.evalVoid("dev.off()", monitor);
		}
		return service.downloadFile(filename, 0, monitor);
	}
	
	public void create(final FunctionCall plot, final OutputStream out, final RService service, final IProgressMonitor monitor) throws CoreException {
		final String filename = "plot-"+System.nanoTime()+".pdf";
		prepare(filename, service, monitor);
		try {
			plot.evalVoid(monitor);
		} finally {
			service.evalVoid("dev.off()", monitor);
		}
		service.downloadFile(out, filename, 0, monitor);
	}
	
	public byte[] create(final String plotCommand, final RService service, final IProgressMonitor monitor) throws CoreException {
		final String filename = "plot-"+System.nanoTime()+".pdf";
		prepare(filename, service, monitor);
		try {
			service.evalVoid(plotCommand, monitor);
		} finally {
			service.evalVoid("dev.off()", monitor);
		}
		return service.downloadFile(filename, 0, monitor);
	}
	
	public void create(final String plotCommand, final OutputStream out, final RService service, final IProgressMonitor monitor) throws CoreException {
		final String filename = "plot-"+System.nanoTime()+".pdf";
		prepare(filename, service, monitor);
		try {
			service.evalVoid(plotCommand, monitor);
		} finally {
			service.evalVoid("dev.off()", monitor);
		}
		service.downloadFile(out, filename, 0, monitor);
	}
	
	@Override
	protected void prepare(final String filename, final RService service, final IProgressMonitor monitor) throws CoreException {
		final FunctionCall png = service.createFunctionCall("CairoPDF");
		png.addChar("file", filename);
//		if (this.resolution > 0) {
//			png.addInt("res", this.resolution);
//		}
		if (unit != null) {
			if (unit.equals(UNIT_IN)) {
				png.addNum("width", width);
				png.addNum("height", height);
			}
			else if (unit.equals(UNIT_CM)) {
				png.addNum("width", width/2.54);
				png.addNum("height", height/2.54);
			}
			else if (unit.equals(UNIT_MM)) {
				png.addNum("width", width/25.4);
				png.addNum("height", height/25.4);
			}
		}
		png.evalVoid(monitor);
	}
	
}
