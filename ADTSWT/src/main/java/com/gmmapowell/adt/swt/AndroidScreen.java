package com.gmmapowell.adt.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class AndroidScreen extends Canvas implements PaintListener {

	private int color = SWT.COLOR_BLACK;

	public AndroidScreen(Composite parent, int options) {
		super(parent, options);
		addPaintListener(this);
	}

	@Override
	public void paintControl(PaintEvent pe) {
		Rectangle rect = ((Control) pe.widget).getBounds();
		pe.gc.setBackground(getDisplay().getSystemColor(color));
		pe.gc.fillRectangle(new Rectangle(0,0, rect.width-1, rect.height-1));
	}

	public void setLayout(SWTADTLayout layout) {
		color = SWT.COLOR_CYAN;
		this.getDisplay().asyncExec(new Runnable() { public void run() { AndroidScreen.this.redraw(); } } );
	}

}
