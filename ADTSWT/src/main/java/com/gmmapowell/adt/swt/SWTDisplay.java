package com.gmmapowell.adt.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.gmmapowell.adt.ScreenRotation;

public class SWTDisplay implements DisposeListener {
	private Display display;
	private Shell shell;
	private Rectangle bounds;
	private AndroidScreen canvas;

	public SWTDisplay(int width, int height, ScreenRotation rotation) {
		display = new Display();
		shell = new Shell(display);
		bounds = new Rectangle(0, 0, width + 300, height+80);
		shell.setBounds(bounds);
		canvas = new AndroidScreen(shell, SWT.NONE);
		canvas.setBounds(new Rectangle(20, 20, width, height));
		shell.setText("Abstract Android Design and Development Tool");
		shell.open();
		shell.addDisposeListener(this);
	}

	public void loop() {
        while (!display.isDisposed()) {
                if (!display.readAndDispatch())
                        display.sleep();
        }

        if (!display.isDisposed())
        {
                System.out.println("Disposing");
                display.dispose();
        }
    }

	@Override
	public void widgetDisposed(DisposeEvent arg0) {
		if (arg0.widget == shell)
			display.dispose();
	}

	public AndroidScreen getCanvas() {
		return canvas;
	}

}
