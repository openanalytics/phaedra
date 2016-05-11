package eu.openanalytics.phaedra.base.ui.volumerenderer;

import java.nio.IntBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.glu.GLU;

import org.eclipse.core.commands.common.EventManager;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.opengl.GLCanvas;
import org.eclipse.swt.opengl.GLData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Slider;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

import com.sun.opengl.util.BufferUtil;

import eu.openanalytics.phaedra.base.ui.volumerenderer.internal.PickHelper;
import eu.openanalytics.phaedra.base.ui.volumerenderer.internal.VolumeRenderer;
import eu.openanalytics.phaedra.base.util.misc.SWTUtils;

public class VolumeViewEditor extends EditorPart implements 
	MouseListener, MouseMoveListener, MouseWheelListener, 
	ISelectionListener, ISelectionProvider {
	
	private Slider sldRotX;
	private Text txtRotX;
	private Slider sldRotY;
	private Text txtRotY;
	private Slider sldRotZ;
	private Text txtRotZ;
	private Button showOverlayBtn;
	
	private GLCanvas canvas;
	private GLContext context;
	private GLU glu;

	private VolumeDataModel model;
	private VolumeRenderer renderer;
	
	private ListenerManager listenerManager;
	private ISelection currentSelection;
	
	private int[] selectionBox;
	private boolean dragging;
	
	private float zoom = 2.00f;
	
	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
		setPartName(input.getName());
	}
	
	@Override
	public void createPartControl(Composite parent) {
		GridLayoutFactory.fillDefaults().applyTo(parent);
		
		GLData data = new GLData();
		data.doubleBuffer = true;
		canvas = new GLCanvas(parent, SWT.NONE, data);
		canvas.setCurrent();
		canvas.addMouseListener(this);
		canvas.addMouseMoveListener(this);
		canvas.addMouseWheelListener(this);
		canvas.addListener(SWT.Resize, new Listener() {
			public void handleEvent(Event event) {
				initPerspective();
				render();
			}
		});
		canvas.addListener(SWT.Paint, new Listener() {
			public void handleEvent(Event event) {
				render();
			}
		});
		GridDataFactory.fillDefaults().grab(true, true).applyTo(canvas);

		Composite sliderContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(sliderContainer);
		GridLayoutFactory.fillDefaults().numColumns(3).margins(5, 5).applyTo(sliderContainer);
		
		// X Rotation
		new Label(sliderContainer, SWT.NONE).setText("X:");
		txtRotX = new Text(sliderContainer, SWT.BORDER);
		txtRotX.setText("0");
		txtRotX.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent e) {
				// Do nothing
			}
			@Override
			public void keyReleased(KeyEvent e) {
				rotationChanged(txtRotX,sldRotX);
			}
		});
		GridDataFactory.fillDefaults().hint(25,SWT.DEFAULT).applyTo(txtRotX);
		sldRotX = new Slider(sliderContainer, SWT.NONE);
		sldRotX.setValues(50, 0, 110, 10, 1, 10);
		sldRotX.addListener (SWT.Selection, new Listener () {
			public void handleEvent (Event event) {
				render();
				txtRotX.setText(""+(50-sldRotX.getSelection()) * 3);
			}
		});
		GridDataFactory.fillDefaults().grab(true, false).applyTo(sldRotX);

		// Y Rotation
		new Label(sliderContainer, SWT.NONE).setText("Y:");
		txtRotY = new Text(sliderContainer, SWT.BORDER);
		txtRotY.setText("0");
		txtRotY.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent e) {
				// Do nothing
			}
			@Override
			public void keyReleased(KeyEvent e) {
				rotationChanged(txtRotY,sldRotY);
			}
		});
		GridDataFactory.fillDefaults().grab(false, false).applyTo(txtRotY);
		sldRotY = new Slider(sliderContainer, SWT.NONE);
		sldRotY.setValues(50, 0, 110, 10, 1, 10);
		sldRotY.addListener (SWT.Selection, new Listener () {
			public void handleEvent (Event event) {
				render();
				txtRotY.setText(""+(50-sldRotY.getSelection()) * 3);
			}
		});
		GridDataFactory.fillDefaults().grab(true, false).applyTo(sldRotY);

		// Z Rotation
		new Label(sliderContainer, SWT.NONE).setText("Z:");
		txtRotZ = new Text(sliderContainer, SWT.BORDER);
		txtRotZ.setText("-90");
		txtRotZ.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent e) {
				// Do nothing
			}
			@Override
			public void keyReleased(KeyEvent e) {
				rotationChanged(txtRotZ,sldRotZ);
			}
		});
		GridDataFactory.fillDefaults().grab(false, false).applyTo(txtRotZ);
		sldRotZ = new Slider(sliderContainer, SWT.NONE);
		sldRotZ.setValues(80, 0, 110, 10, 1, 10);
		sldRotZ.addListener (SWT.Selection, new Listener () {
			public void handleEvent (Event event) {
				render();
				txtRotZ.setText(""+(50-sldRotZ.getSelection()) * 3);
			}
		});
		GridDataFactory.fillDefaults().grab(true, false).applyTo(sldRotZ);

		showOverlayBtn = new Button(parent, SWT.CHECK);
		showOverlayBtn.setText("Show Overlay");
		showOverlayBtn.setSelection(true);
		showOverlayBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				renderer.enableOverlay(showOverlayBtn.getSelection());
				render();
				// For some reason, sometimes nothing is rendered
				// immediately after a change to the texture buffers.
				render();
			}
		});
		GridDataFactory.fillDefaults().grab(false, false).applyTo(showOverlayBtn);

		initGLContext();
		
		listenerManager = new ListenerManager();
		getSite().setSelectionProvider(this);
		getSite().getPage().addSelectionListener(this);
		
		selectionBox = new int[4];
		
		//Get the data model and renderer from the EditorInput
		model = ((VolumeViewEditorInput)getEditorInput()).getDataModel();
		renderer = (VolumeRenderer)model.getRenderer();
		Job loadImageJob = new Job("Volume Viewer") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask("Loading Image " + model.getImagePath(), IProgressMonitor.UNKNOWN);
				renderer.init(null, null);
				render(true);
				render(true);
				return Status.OK_STATUS;
			}
		};
		loadImageJob.setUser(true);
		loadImageJob.schedule();
	}

	@Override
	public void setFocus() {
		canvas.setFocus();
	}

	@Override
	public void dispose() {
		getSite().getPage().removeSelectionListener(this);
		getSite().setSelectionProvider(null);
	}
	
	/*
	 * *********************
	 * OpenGL initialization
	 * *********************
	 */
	
	protected void initGLContext() {
		// Initialize the OpenGL context, taken from the GLCanvas.
		context = GLDrawableFactory.getFactory().createExternalGLContext();
		context.makeCurrent();
		GL gl = context.getGL();
		gl.glHint(GL.GL_PERSPECTIVE_CORRECTION_HINT, GL.GL_NICEST);
		gl.glClearDepth(1.0);
		gl.glEnable(GL.GL_POINT_SMOOTH);
		gl.glEnable(GL.GL_LINE_SMOOTH);
		gl.glEnable(GL.GL_DEPTH_TEST);
		gl.glEnable(GL.GL_BLEND);
		gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
		context.release();
		glu = new GLU();
	}
	
	protected void initPerspective() {
		// Create a perspective using the current view size.
		Rectangle bounds = canvas.getBounds();
		float fAspect = (float) bounds.width / (float) bounds.height;
		canvas.setCurrent();
		context.makeCurrent();
		GL gl = context.getGL();
		gl.glMatrixMode(GL.GL_PROJECTION);
		gl.glLoadIdentity();
		gl.glViewport(0, 0, bounds.width, bounds.height);
		glu.gluPerspective(45.0f, fAspect, 0.5f, 10000f);
		gl.glMatrixMode(GL.GL_MODELVIEW);
		gl.glLoadIdentity();
		context.release();
	}
	
	/*
	 * ************
	 * Render steps
	 * ************
	 */
	
	protected void render(boolean async) {
		if (async) {
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					render();								
				}
			});
		} else {
			render();
		}
	}
	
	protected void render() {
		if (!canvas.isDisposed()) {
			GL gl = renderBegin();
			positionCamera(gl);
			drawScene(gl);
			renderEnd(gl);
		}
	}

	protected GL renderBegin() {
		canvas.setCurrent();
		context.makeCurrent();
		GL gl = context.getGL();
		return gl;
	}
	
	protected void positionCamera(GL gl) {
		gl.glPushMatrix();
		
		// Camera location
		double distance = 200*zoom;
		glu.gluLookAt(
				0,0,distance,
				0,0,0,
				0d,1d,0d);
		
		// Rotation.
		int diffX = (50-sldRotX.getSelection()) * 3;
		gl.glRotatef(diffX, 1f,0f,0f);
		int diffY = (50-sldRotY.getSelection()) * 3;
		gl.glRotatef(diffY, 0f,1f,0f);
		int diffZ = (50-sldRotZ.getSelection()) * 3;
		gl.glRotatef(diffZ, 0f,0f,1f);
		renderer.updateRotation(diffX,diffY,diffZ);

		// Background color
		gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
		gl.glClearColor(.8f, .8f, .8f, 1.0f);
	}

	protected void drawScene(GL gl) {
		renderer.render(gl, null);
	}
	
	protected void drawSelectionBox(GL gl) {
		if (selectionBox[2] - selectionBox[0] != 0) {
			gl.glLoadName(-1);
			gl.glMatrixMode(GL.GL_PROJECTION);
			gl.glPushMatrix();
			gl.glLoadIdentity();
			gl.glOrtho(0, canvas.getBounds().width, canvas.getBounds().height, 0, -1, 1);
			gl.glMatrixMode(GL.GL_MODELVIEW);
			
			gl.glColor4f(0f,0f,0.4f,0.2f);
			Rectangle rect = SWTUtils.create(selectionBox);
			gl.glRecti(rect.x,rect.y,rect.x+rect.width,rect.y+rect.height);
			
			gl.glMatrixMode(GL.GL_PROJECTION);
			gl.glPopMatrix();
			gl.glMatrixMode(GL.GL_MODELVIEW);
		}
	}
	
	protected void renderEnd(GL gl) {
		gl.glPopMatrix();
		
		drawSelectionBox(gl);
		
		canvas.swapBuffers();
		context.release();
	}
	
	/*
	 * ************************
	 * OpenGL Selection handler
	 * ************************
	 */
	
	private void pickData(int mouseX, int mouseY) {
		GL gl = renderBegin();

		int maxSelectCount = 1024;
		IntBuffer viewPort = IntBuffer.allocate(4);
		gl.glGetIntegerv(GL.GL_VIEWPORT, viewPort);
		
		IntBuffer selectBuffer = BufferUtil.newIntBuffer(maxSelectCount);
		gl.glSelectBuffer(maxSelectCount, selectBuffer);
		gl.glRenderMode(GL.GL_SELECT);
		gl.glInitNames();
		gl.glPushName(-1);
		
		gl.glMatrixMode(GL.GL_PROJECTION);
		gl.glPushMatrix();
		gl.glLoadIdentity();

		boolean multiPick = true;
		Rectangle selectionRect = SWTUtils.create(selectionBox);
		if (selectionRect.width < 5 && selectionRect.height < 5) {
			selectionRect = SWTUtils.create(
					selectionBox[0]-5,selectionBox[1]-5,
					selectionBox[2]+5,selectionBox[3]+5);
			//multiPick = false;
		}
		int[] pickMatrix = PickHelper.createPickMatrix(selectionRect, viewPort, multiPick);
		glu.gluPickMatrix(pickMatrix[0],pickMatrix[1],pickMatrix[2],pickMatrix[3],viewPort);

		float fAspect = (float)(viewPort.get(2)-viewPort.get(0))/(viewPort.get(3)-viewPort.get(1));
		glu.gluPerspective(45.0f, fAspect, 0.5f, 10000f);
		
		gl.glMatrixMode(GL.GL_MODELVIEW);
		gl.glLoadIdentity();

		positionCamera(gl);
		renderer.renderPoints(gl, null);
		gl.glPopMatrix();
		
		gl.glMatrixMode(GL.GL_PROJECTION);
		gl.glPopMatrix();
		gl.glMatrixMode(GL.GL_MODELVIEW);
		int hits = gl.glRenderMode(GL.GL_RENDER);
		
		context.release();
		
		// Find the closest hit (nearest to the camera position).
		if (hits > 0 && model != null) {
			int[] hitNames = PickHelper.pick(hits, selectBuffer, multiPick);
			handlePick(hitNames);
		} else {
			currentSelection = null;
			for (VolumeDataItem item: model.getItems()) {
				item.setSelected(false);
			}
		}
		fireSelectionChanged();
	}
	
	protected void handlePick(int[] names) {
		currentSelection = model.handleInternalSelection(names);
	}
	
	/* 
	 * **************************
	 * Mouse handlers
	 * **************************
	 */
	
	@Override
	public void mouseDoubleClick(MouseEvent e) {
		// Do nothing.
	}

	@Override
	public void mouseDown(MouseEvent e) {
		canvas.setFocus();
		dragging = true;
		// Start dragging a selection box.
		selectionBox[0] = e.x;
		selectionBox[1] = e.y;
		selectionBox[2] = e.x;
		selectionBox[3] = e.y;
	}

	@Override
	public void mouseUp(MouseEvent e) {
		dragging = false;
		pickData(e.x,e.y);
		selectionBox[0] = 0;
		selectionBox[1] = 0;
		selectionBox[2] = 0;
		selectionBox[3] = 0;
		render();
	}

	@Override
	public void mouseMove(MouseEvent e) {
		if (dragging) {
			// Update the selection box
			selectionBox[2] = e.x;
			selectionBox[3] = e.y;
			render();
		}
	}

	@Override
	public void mouseScrolled(MouseEvent e) {
		int diff = e.count;
		if (diff < 0) zoom += 0.1;
		else zoom -= 0.1;
		// Do not allow zoom to go below 0.1
		if (zoom < 0.1f) zoom = 0.1f;
		render();
	}

	/*
	 * ******************
	 * Selection provider
	 * ******************
	 */
	
	private void fireSelectionChanged() {
		ISelection sel = currentSelection;
		if (sel == null) sel = new StructuredSelection();
		final SelectionChangedEvent e = new SelectionChangedEvent(this, sel);
		listenerManager.fireSelectionChangedEvent(e);
	}
	
	@Override
	public ISelection getSelection() {
		return null;
	}
	
	@Override
	public void setSelection(ISelection selection) {
		// Do nothing.
	}
	
	@Override
	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		listenerManager.addSelectionChangedListener(listener);
	}

	@Override
	public void removeSelectionChangedListener(ISelectionChangedListener listener) {
		listenerManager.removeSelectionChangedListener(listener);
	}

	/*
	 * ******************
	 * Selection listener
	 * ******************
	 */

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if (model != null) {
			model.handleExternalSelection(selection);
			render();
		}
	}
	
	private void rotationChanged(Text rotTxt, Slider rotSld) {
		try {
			int val = Integer.parseInt(rotTxt.getText());
			val = 50-(val/3);
			rotSld.setSelection(val);
			render();
		} catch (NumberFormatException ex) {}
	}
	
	private class ListenerManager extends EventManager {
		
		public void addSelectionChangedListener(ISelectionChangedListener listener) {
			addListenerObject(listener);
		}
		
		public void removeSelectionChangedListener(ISelectionChangedListener listener) {
			removeListenerObject(listener);
		}
		
		public void fireSelectionChangedEvent(SelectionChangedEvent e) {
			for (Object o: getListeners()) {
				((ISelectionChangedListener)o).selectionChanged(e);
			}
		}
	}

	/*
	 * **********************
	 * Save methods: disabled
	 * **********************
	 */
	
	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}
	
	@Override
	public void doSave(IProgressMonitor monitor) {
		// Do nothing: this editor does not affect state.
	}

	@Override
	public void doSaveAs() {
		// Do nothing: this editor does not affect state.
	}
}
