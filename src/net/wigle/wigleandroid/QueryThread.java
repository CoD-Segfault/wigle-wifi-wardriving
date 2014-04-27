package net.wigle.wigleandroid;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class QueryThread extends Thread {
  private final BlockingQueue<Request> queue = new LinkedBlockingQueue<Request>();
  private final AtomicBoolean done = new AtomicBoolean( false );
  private final DatabaseHelper dbHelper;
  
  public interface ResultHandler {
    public void handleRow( Cursor cursor );
    public void complete();
  }
  public static class Request {
    private final String sql;
    private final ResultHandler handler;
    
    public Request( final String sql, final ResultHandler handler ) {
      if ( sql == null ) {
        throw new IllegalArgumentException( "sql is null" );
      }
      if ( handler == null ) {
        throw new IllegalArgumentException( "handler is null" );
      }
      this.sql = sql;
      this.handler = handler;
    }
  }
  
  public QueryThread( final DatabaseHelper dbHelper ) {
    this.dbHelper = dbHelper;
    setName( "query-" + getName() );
  }
  
  public void setDone() {
    done.set( true );
  }
  
  public void addToQueue( final Request request ) {
    try {
      queue.put( request );
    }
    catch ( InterruptedException ex ) {
      MainActivity.info( getName() + " interrupted" );
    }
  }
  
  public void run() {
    while ( ! done.get() ) {
      try {
        final Request request = queue.take();
        // if(true) throw new DBException("meh", new SQLiteException("meat puppets"));
        if ( request != null ) {
          final SQLiteDatabase db = dbHelper.getDB();
          if ( db != null ) {
            final Cursor cursor = db.rawQuery( request.sql, null );
            while ( cursor.moveToNext() ) {
              request.handler.handleRow( cursor );
            }
            request.handler.complete();
            cursor.close();
          }
        }
      }
      catch ( InterruptedException ex ) {
        MainActivity.info( getName() + " interrupted" );
      }
      catch ( DBException ex ) {
        dbHelper.deathDialog("query thread", ex);        
      }
    }
  }
  
}
