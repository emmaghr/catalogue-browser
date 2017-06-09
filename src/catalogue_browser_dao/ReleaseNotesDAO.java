package catalogue_browser_dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;

import catalogue.Catalogue;
import catalogue.ReleaseNotes;
import catalogue.ReleaseNotesOperation;

/**
 * Dao to get the release note information from the database
 * @author avonva
 *
 */
public class ReleaseNotesDAO {

	private Catalogue catalogue;
	
	/**
	 * Initialize the dao with the catalogue we want to work with
	 * @param catalogue
	 */
	public ReleaseNotesDAO( Catalogue catalogue ) {
		this.catalogue = catalogue;
	}
	
	/**
	 * Insert the catalogue release notes into the db
	 * @return
	 */
	public boolean insert( ReleaseNotes notes ) {
		
		String query = "update APP.CATALOGUE set CAT_RN_DESCRIPTION=?, "
				+ "CAT_RN_VERSION_DATE=?,"
				+ " CAT_RN_INTERNAL_VERSION=?,"
				+ "CAT_RN_INTERNAL_VERSION_NOTE=? "
				+ "where CAT_ID = ?";
		
		try {
			
			Connection con = DatabaseManager.getMainDBConnection();
			PreparedStatement stmt = con.prepareStatement( query );

			stmt.setString( 1, notes.getDescription() );
			stmt.setTimestamp( 2, notes.getDate() );
			stmt.setString( 3, notes.getInternalVersion() );
			stmt.setString( 4, notes.getInternalVersionNote() );
			stmt.setInt( 5, catalogue.getId() );
			
			stmt.executeUpdate();
			
			stmt.close();
			con.close();
			
			return true;
			
		} catch ( SQLException e ) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	/**
	 * Get the catalogue release notes
	 * @return
	 */
	public ReleaseNotes getReleaseNotes() {
		
		ReleaseNotes rn = null;
		
		String query = "select CAT_RN_DESCRIPTION, CAT_RN_VERSION_DATE, CAT_RN_INTERNAL_VERSION, "
				+ "CAT_RN_INTERNAL_VERSION_NOTE from APP.CATALOGUE where CAT_ID = ?";
		
		try {
			
			Connection con = DatabaseManager.getMainDBConnection();
			PreparedStatement stmt = con.prepareStatement( query );
			
			stmt.setInt( 1, catalogue.getId() );
			
			ResultSet rs = stmt.executeQuery();
			
			if ( rs.next() ) {
			
				String desc = rs.getString( "CAT_RN_DESCRIPTION" );
				Timestamp date = rs.getTimestamp( "CAT_RN_VERSION_DATE" );
				String intVersion = rs.getString( "CAT_RN_INTERNAL_VERSION" );
				String note = rs.getString( "CAT_RN_INTERNAL_VERSION_NOTE" );
				
				ReleaseNotesOperationDAO opDao = new ReleaseNotesOperationDAO( catalogue );
				Collection<ReleaseNotesOperation> ops = opDao.getAll();
				
				// create the release note
				rn = new ReleaseNotes( desc, date, intVersion, note, ops );
			}
			
			rs.close();
			stmt.close();
			con.close();
		} catch ( SQLException e ) {
			e.printStackTrace();
		}
		
		return rn;
	}
}