package eu.openanalytics.phaedra.base.ui.charting.v2.data;

import java.io.IOException;
import java.util.BitSet;

import javax.swing.BoundedRangeModel;

import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.topcat.RowSubset;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.topcat.plot.CartesianPointStore;
import uk.ac.starlink.topcat.plot.ErrorModeSelectionModel;
import uk.ac.starlink.topcat.plot.PointStore;
import uk.ac.starlink.topcat.plot.Points;
import uk.ac.starlink.ttools.plot.ErrorMode;
import uk.ac.starlink.ttools.plot.PlotData;
import uk.ac.starlink.ttools.plot.PointSequence;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot.WrapperPlotData;

/**
 * Encapsulates the selection of the list of points which is to be plotted.
 * This may be composed of points from one or more than one tables.
 *
 * @author   Mark Taylor
 * @since    2 Nov 2005
 */
public class SimplePlotData implements PlotData {

	private final int ndim_;
	private final int nTable_;
	private final long[] nrows_;
	private RowSubset[] subsets;
	private Style[] styles;
	private Points points_;
	private Style style;

	private StarTable currentTable;

	/**
	 * Constructs a new selection.
	 *
	 * <p>As well as the point selectors themselves which hold almost all
	 * the required state, an additional array, <code>subsetPointers</code>
	 * is given to indicate in what order the subsets should be plotted.
	 * Each element of this array is a two-element int array; the first
	 * element is the index of the point selector, and the second element
	 * the index of the subset within that selector.
	 *
	 * @param  selectors  array of PointSelector objects whose current state
	 *         determines the points to be plotted
	 * @param  subsetPointers  pointers to subsets
	 * @param  subsetNames     labels to be used for the subsets in 
	 *                         <code>subsetPointers</code>
	 */
	public SimplePlotData(int[][] subsetPointers,String[] subsetNames, StarTable currentTable, int dimensions ) {
		this.currentTable = currentTable;
		nTable_ = 1;
		ndim_ = dimensions;
		nrows_ = new long[ nTable_ ];

		/* Set dummy points object. */
		points_ = getEmptyPoints();
	}
	/**
	 * adds a subset to the list of subsets
	 * 
	 * @param name the name of the subset
	 * @param bits a bitset which contains all rows, rows to be painted will be true, the rest false
	 */
	public void setSubsets(RowSubset[] subsets){
		this.subsets = subsets;
	}
	public RowSubset[] getSubsets(){
		return subsets;
	}
	public void setStyles(Style[] styles){
		this.styles = styles;
	}
	public static ErrorModeSelectionModel[] createErrorModeModels(
			String[] axisNames ) {
		int nerror = axisNames.length;
		ErrorModeSelectionModel[] errorModeModels =
				new ErrorModeSelectionModel[ nerror ];
		for ( int ierr = 0; ierr < nerror; ierr++ ) {
			errorModeModels[ ierr ] =
					new ErrorModeSelectionModel( ierr, axisNames[ ierr ] );
		}
		return errorModeModels;
	}
	public ErrorMode[] getErrorModes() {
		ErrorModeSelectionModel[] errorModeModels = createErrorModeModels(new String[]{"X","Y"});
		int nerr = errorModeModels.length;
		ErrorMode[] modes = new ErrorMode[ nerr ];
		for ( int ierr = 0; ierr < nerr; ierr++ ) {
			modes[ ierr ] = errorModeModels[ ierr ].getErrorMode();
		}
		return modes;
	}
	/** 
	 * Reads a data points list for this selection.  The data are actually
	 * read from the table objects in this call, so it may be time-consuming.
	 * So, don't call it if you already have the data it would return
	 * (see {@link #sameData}).  If a progress bar model is supplied it
	 * will be updated as the read progresses.
	 *
	 * <p>This method checks for interruption status on its calling thread.
	 * If an interruption is made, it will cease calculating and throw
	 * an InterruptedException.
	 *
	 * @param    progress bar model to be updated as read is done
	 * @return   points list
	 * @throws   InterruptedException  if the calling thread is interrupted
	 */
	public Points readPoints( BoundedRangeModel progress )
			throws IOException, InterruptedException {
		int npoint = 0;
		for ( int itab = 0; itab < nTable_; itab++ ) {
			npoint += currentTable.getRowCount();
		}
		ErrorMode[] errorModes = getErrorModes();
		PointStore pointStore = new CartesianPointStore(ndim_, errorModes, npoint);
		if ( progress != null ) {
			progress.setMinimum( 0 );
			progress.setMaximum( npoint );
		}
		int step = Math.max( npoint / 100, 1000 );
		int ipoint = 0;
		for ( int itab = 0; itab < nTable_; itab++ ) {
			RowSequence datSeq = null;
			RowSequence errSeq = null;
			RowSequence labSeq = null;
			try {
				datSeq = currentTable.getRowSequence();
				while ( datSeq.next() ) {
					Object[] datRow = datSeq.getRow();
					Object[] errRow;
					String label;
					if ( errSeq == null ) {
						errRow = null;
					}
					else {
						boolean hasNext = errSeq.next();
						assert hasNext;
						errRow = errSeq.getRow();
					}
					if ( labSeq == null ) {
						label = null;
					}
					else {
						boolean hasNext = labSeq.next();
						assert hasNext;
						Object obj = labSeq.getCell( 0 );
						label = obj == null ? null : obj.toString();
					}

					pointStore.storePoint( datRow, errRow, label );
					ipoint++;

					if ( ipoint % step == 0 ) {
						if ( progress != null ) {
							progress.setValue( ipoint );
						}
						if ( Thread.interrupted() ) {
							throw new InterruptedException();
						}
					}
				}
				assert ( errSeq == null ) || ( ! errSeq.next() );
				assert ( labSeq == null ) || ( ! labSeq.next() );
			}

			finally {
				if ( datSeq != null ) {
					assert ! datSeq.next();
					datSeq.close();
				}
				if ( errSeq != null ) {
					assert ! errSeq.next();
					errSeq.close();
				}
				if ( labSeq != null ) {
					assert ! labSeq.next();
					labSeq.close();
				}
			}
		}
		assert ipoint == npoint;
		return pointStore;
	}

	/**
	 * Returns a dummy Points object compatible with this selection.
	 * It contains no data.
	 *
	 * @return   points object with <code>getCount()==0</code>
	 */
	public Points getEmptyPoints() {
		return new EmptyPoints();
	}

	/**
	 * Returns a list of styles for subset plotting.
	 * This corresponds to the subset list returned by 
	 * {@link #getSubsets}.
	 *
	 * @return  style array
	 */
	public Style[] getStyles() {
		return styles;
	}

	public int getSetCount() {
		if(subsets == null)
			return 1;
		else
			return subsets.length;
	}

	public String getSetName( int iset ) {
		if(subsets == null)
			return "All";
		else
			return subsets[ iset ].getName();
	}

	public Style getSetStyle( int iset ) {   
		if(styles == null || styles.length == 0)
			return style;
		else
			return styles[iset];
	}
	public void setStyle(Style style){
		this.style = style;
	}
	public int getNdim() {
		return points_.getNdim();
	}

	public int getNerror() {
		return points_.getNerror();
	}

	public boolean hasLabels() {
		return points_.hasLabels();
	}

	/**
	 * Returns a PlotData object based on this point selection but with a 
	 * given points object.  Since PointSelection implements PlotData in
	 * any case, this is not always necessary, but what this method provides
	 * is a PlotData whose data will not change if the points object owned
	 * by this PointSelection is replaced.
	 *
	 * @param  points  fixed points data
	 * @return   plot data closure
	 */
	public PlotData createPlotData( final Points points ) {
		return new WrapperPlotData( this ) {
			public int getNdim() {
				return points.getNdim();
			}
			public int getNerror() {
				return points.getNerror();
			}
			public boolean hasLabels() {
				return points.hasLabels();
			}
			public PointSequence getPointSequence() {
				return new SelectionPointSequence( points );
			}
		};
	}

	public PointSequence getPointSequence() {
		return new SelectionPointSequence( points_ );
	}

	public void setPoints( Points points ) {
		points_ = points;
	}

	public Points getPoints() {
		return points_;
	}

	/**
	 * Given a point index from this selection, returns the row number
	 * in its table (see {@link #getPointTable} that it represents.
	 *
	 * @param  ipoint  point index
	 * @return  row number of point index in its table
	 */
	public long getPointRow( long ipoint ) {
		for ( int itab = 0; itab < nTable_; itab++ ) {
			if ( ipoint >= 0 && ipoint < nrows_[ itab ] ) {
				return ipoint;
			}
			ipoint -= nrows_[ itab ];
		}
		return -1L;
	}

	/**
	 * PointSequence implementation used by a PointSelection.
	 */
	private final class SelectionPointSequence implements PointSequence {

		private final Points psPoints_;
		private final int npoint_;
		private int ip_ = -1;

		/**
		 * Constructor.
		 */
		SelectionPointSequence( Points points ) {
			psPoints_ = points == null ? new EmptyPoints() : points;
			npoint_ = psPoints_.getCount();
		}

		public boolean next() {
			return ++ip_ < npoint_;
		}

		public double[] getPoint() {
			return psPoints_.getPoint( ip_ );
		}

		public double[][] getErrors() {
			return psPoints_.getErrors( ip_ );
		}

		public String getLabel() {
			return psPoints_.getLabel( ip_ );
		}

		public boolean isIncluded( int iset ) {
			boolean isIncluded;
			if(subsets == null)
				isIncluded = true;        	
			else
				isIncluded = subsets[iset].isIncluded( ip_ );         	
			return isIncluded;
		}

		public void close() {
			ip_ = Integer.MIN_VALUE;
			return;
		}
	}

	/**
	 * Struct-type class which defines an association of a TopcatModel
	 * and a BitSet.
	 */
	public static class TableMask {
		private final TopcatModel tcModel_;
		private final BitSet mask_;

		/**
		 * Constructor.
		 *
		 * @param  tcModel   table model
		 * @param  mask   bit vector
		 */
		private TableMask( TopcatModel tcModel, BitSet mask ) {
			tcModel_ = tcModel;
			mask_ = mask;
		}

		/**
		 * Returns the table.
		 *
		 * @return  topcat model
		 */
		public TopcatModel getTable() {
			return tcModel_;
		}

		/**
		 * Returns the bit mask.
		 *
		 * @return  bit set
		 */
		public BitSet getMask() {
			return mask_;
		}
	}

	/**
	 * Points implementation with no data.
	 */
	private class EmptyPoints implements Points {
		public int getNdim() {
			return ndim_;
		}
		public int getNerror() {
			return 0;
		}
		public int getCount() {
			return 0;
		}
		public double[] getPoint( int ipoint ) {
			throw new IllegalArgumentException( "no data" );
		}
		public double[][] getErrors( int ipoint ) {
			throw new IllegalArgumentException( "no data" );
		}
		public boolean hasLabels() {
			return false;
		}
		public String getLabel( int ipoint ) {
			return null;
		}
	}
}