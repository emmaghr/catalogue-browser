package ui_licence;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class FormBrowser {

	private static final Logger LOGGER = LogManager.getLogger(FormBrowser.class);
	
	String		_title;
	String		_url	= null;
	InputStream	_inputStream;
	Shell		dialog;
	Browser		_browser;

	public FormBrowser( String title, String url ) {
		_title = title;
		_url = url;
	}

	public FormBrowser( String title, InputStream inputStream ) {
		_title = title;
		_inputStream = inputStream;
	}

	public void display (Shell shell ) {
		
		dialog = new Shell(shell, SWT.SHEET | SWT.APPLICATION_MODAL | SWT.WRAP | SWT.BORDER | SWT.TITLE);
		
		dialog.setImage( new Image( Display.getCurrent() , this.getClass().getClassLoader()
				.getResourceAsStream( "Print24.gif" ) ) );
		
		dialog.setSize(650, 500);

		dialog.setText( _title );
		dialog.setLayout( new GridLayout( 1 , false ) );
		
		_browser = new Browser( dialog , SWT.NONE );
		GridData gridData = new GridData();
		gridData.verticalAlignment = SWT.FILL;
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		_browser.setLayoutData( gridData );
		if ( _url != null )
			_browser.setUrl( _url );
		else {
			char[] buf = new char[2048];
			Reader r;
			try {
				r = new InputStreamReader( _inputStream , "UTF-8" );
			} catch ( UnsupportedEncodingException e ) {
				LOGGER.error("Cannot read input stream with UTF-8", e);
				e.printStackTrace();
				r = null;
			}
			StringBuilder s = new StringBuilder();
			while ( true ) {
				int n = -1;
				try {
					n = r.read( buf );
				} catch ( IOException e ) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					LOGGER.error("Cannot read", e);
				}
				if ( n < 0 )
					break;

				s.append( buf, 0, n );

			}

			String htmlText = "<html><header><title>Derby Notice</title></header><body><pre>" + s.toString()
					+ "</pre></body></html>";
			_browser.setText( htmlText );
		}

		dialog.open();
	}

}
