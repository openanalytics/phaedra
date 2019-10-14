package eu.openanalytics.phaedra.base.ui.util.autocomplete;

import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Control;

/**
 * ComboAutoCompleteField is a class which attempts to auto-complete a user's
 * keystrokes in the Combo or ComboViewer by activating a popup that filters a list of proposals
 * according to the content typed by the user.
 *
 * Supports ? and * wildcards.
 *
 * @see ContentProposalAdapter
 * @see WildcardContentProposalProvider
 *
 * @since 3.3
 */
public class ComboAutoCompleteField {

	private WildcardContentProposalProvider proposalProvider;
	private ContentProposalAdapter adapter;

	private int lastSelectedIndex = -1;

	/**
	 * Construct an ComboAutoCompleteField field on the specified Combo, whose
	 * completions are characterized by the specified array of Strings.
	 *
	 * ComboViewer should use the other constructor of this class.
	 *
	 * @param combo
	 *            the ComboViewer for which autocomplete is desired. May not be
	 *            <code>null</code>.
	 * @param proposals
	 *            the array of Strings representing valid content proposals for
	 *            the field.
	 */
	public ComboAutoCompleteField(Combo combo) {
		proposalProvider = new WildcardContentProposalProvider(combo.getItems());
		adapter = new ContentProposalAdapter(combo, new ComboContentAdapter(),
				proposalProvider, null, null);
		adapter.setPropagateKeys(true);
		adapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);
	}

	/**
	 * Construct an ComboAutoCompleteField field on the specified ComboViewer, whose
	 * completions are characterized by the specified array of Strings.
	 *
	 * @param comboViewer
	 *            the ComboViewer for which autocomplete is desired. May not be
	 *            <code>null</code>.
	 */
	public ComboAutoCompleteField(ComboViewer comboViewer) {
		this(comboViewer, comboViewer.getCombo().getItems());
	}

	/**
	 * Construct an ComboAutoCompleteField field on the specified ComboViewer, whose
	 * completions are characterized by the specified array of Strings.
	 *
	 * @param comboViewer
	 *            the ComboViewer for which autocomplete is desired. May not be
	 *            <code>null</code>.
	 * @param proposals
	 *            the array of Strings representing valid content proposals for
	 *            the field.
	 */
	public ComboAutoCompleteField(ComboViewer comboViewer, String[] proposals) {
		proposalProvider = new WildcardContentProposalProvider(proposals);
		adapter = new ContentProposalAdapter(comboViewer.getControl(), new ComboContentAdapter(),
				proposalProvider, null, null);
		adapter.setPropagateKeys(true);
		adapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);

		comboViewer.getControl().addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				Combo combo = (Combo) e.getSource();
				// If the combo has no selection when focus is lost, attempt to restore last known selection.
				if (lastSelectedIndex >= 0 && combo.getSelectionIndex() < 0) {
					combo.select(lastSelectedIndex);
				}
			}
		});
		comboViewer.getControl().addKeyListener(new KeyListener() {
			@Override
			public void keyReleased(KeyEvent e) {
				Combo combo = (Combo) e.getSource();
				// If the combo has no selection and the user presses Esc or Enter, restore last known selection.
				if (lastSelectedIndex >= 0 && combo.getSelectionIndex() < 0) {
					if (e.character == SWT.CR || e.character == SWT.LF || e.character == SWT.ESC) {
						combo.select(lastSelectedIndex);
					}
				}
			}
			@Override
			public void keyPressed(KeyEvent e) {
				// Store the last known selected index, if any.
				int selectionIndex = -1;
				Control control = comboViewer.getControl();
				if (control instanceof Combo) selectionIndex = comboViewer.getCombo().getSelectionIndex();
				if (control instanceof CCombo) selectionIndex = comboViewer.getCCombo().getSelectionIndex(); 
				if (selectionIndex >= 0) {
					lastSelectedIndex = selectionIndex;
				}
			}
		});
	}

	/**
	 * Set the Strings to be used as content proposals.
	 *
	 * @param proposals
	 *            the array of Strings to be used as proposals.
	 */
	public void setProposals(String[] proposals) {
		proposalProvider.setProposals(proposals);
	}

}
