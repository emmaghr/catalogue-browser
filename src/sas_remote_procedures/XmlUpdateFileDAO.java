package sas_remote_procedures;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

import catalogue.Catalogue;
import catalogue_browser_dao.CatalogueDAO;
import catalogue_browser_dao.CatalogueEntityDAO;
import catalogue_browser_dao.DatabaseManager;

/**
 * Dao to communicate with the CAT_UPDATES_XML table
 * @author avonva
 *
 */
public class XmlUpdateFileDAO implements CatalogueEntityDAO<XmlUpdateFile> {

	@Override
	public int insert(XmlUpdateFile object) {

		int id = object.getCatalogue().getId();

		String query = "insert into APP.CAT_UPDATES_XML (CAT_ID, XML_FILENAME) values (?,?)";

		try {
			Connection con = DatabaseManager.getMainDBConnection();
			PreparedStatement stmt = con.prepareStatement( query );

			stmt.setInt( 1, object.getCatalogue().getId() );
			stmt.setString( 2, object.getXmlFilename() );

			stmt.executeUpdate();

			stmt.close();
			con.close();

		} catch ( SQLException e ) {
			e.printStackTrace();
		}

		return id;
	}

	@Override
	public boolean remove(XmlUpdateFile object) {
		
		String query = "delete from APP.CAT_UPDATES_XML where CAT_ID = ?";

		try {
			Connection con = DatabaseManager.getMainDBConnection();
			PreparedStatement stmt = con.prepareStatement( query );

			stmt.setInt( 1, object.getCatalogue().getId() );

			stmt.executeUpdate();

			stmt.close();
			con.close();
			
			return true;

		} catch ( SQLException e ) {
			e.printStackTrace();
		}
		
		return false;
	}

	@Override
	public boolean update(XmlUpdateFile object) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public XmlUpdateFile getById(int id) {
		
		XmlUpdateFile obj = null;
		
		String query = "select * from APP.CAT_UPDATES_XML where CAT_ID = ?";

		try {
			
			Connection con = DatabaseManager.getMainDBConnection();
			PreparedStatement stmt = con.prepareStatement( query );

			stmt.setInt( 1, id );

			ResultSet rs = stmt.executeQuery();

			if ( rs.next() )
				obj = getByResultSet( rs );
			
			rs.close();
			stmt.close();
			con.close();

		} catch ( SQLException e ) {
			e.printStackTrace();
		}
		
		return obj;
	}

	@Override
	public XmlUpdateFile getByResultSet(ResultSet rs) throws SQLException {
		int catId = rs.getInt( "CAT_ID" );
		String xmlFilename = rs.getString( "XML_FILENAME" );

		CatalogueDAO catDao = new CatalogueDAO();
		Catalogue catalogue = catDao.getById( catId );

		return new XmlUpdateFile( catalogue, xmlFilename );
	}

	@Override
	public Collection<XmlUpdateFile> getAll() {
		
		Collection<XmlUpdateFile> objs = new ArrayList<>();
		
		String query = "select * from APP.CAT_UPDATES_XML";

		try {
			
			Connection con = DatabaseManager.getMainDBConnection();
			PreparedStatement stmt = con.prepareStatement( query );

			ResultSet rs = stmt.executeQuery();

			while ( rs.next() )
				objs.add( getByResultSet( rs ) );
			
			rs.close();
			stmt.close();
			con.close();

		} catch ( SQLException e ) {
			e.printStackTrace();
		}
		
		return objs;
	}

}