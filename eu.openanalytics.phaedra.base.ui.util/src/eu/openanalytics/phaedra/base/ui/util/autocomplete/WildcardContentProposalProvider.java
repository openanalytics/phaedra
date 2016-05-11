package eu.openanalytics.phaedra.base.ui.util.autocomplete;

import java.util.ArrayList;

import org.eclipse.jface.fieldassist.ContentProposal;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;

import eu.openanalytics.phaedra.base.ui.util.misc.StringMatcher;

/**
 * WildcardContentProposalProvider is a class designed to map a static list of
 * Strings to content proposals.
 *
 * Supports ? and * wildcards.
 *
 * @see IContentProposalProvider
 * @since 3.2
 *
 */
public class WildcardContentProposalProvider implements IContentProposalProvider {

	/*
	 * The proposals provided.
	 */
	private String[] proposals;

	/*
	 * The proposals mapped to IContentProposal. Cached for speed in the case
	 * where filtering is not used.
	 */
	private IContentProposal[] contentProposals;

	/*
	 * Boolean that tracks whether filtering is used.
	 */
	private boolean filterProposals = true;

	/**
	 * Construct a SimpleContentProposalProvider whose content proposals are
	 * always the specified array of Objects.
	 *
	 * @param proposals
	 *            the array of Strings to be returned whenever proposals are
	 *            requested.
	 */
	public WildcardContentProposalProvider(String[] proposals) {
		super();
		this.proposals = proposals;
	}

	/**
	 * Return an array of Objects representing the valid content proposals for a
	 * field.
	 *
	 * @param contents
	 *            the current contents of the field (only consulted if filtering
	 *            is set to <code>true</code>)
	 * @param position
	 *            the current cursor position within the field (ignored)
	 * @return the array of Objects that represent valid proposals for the field
	 *         given its current content.
	 */
	@Override
	public IContentProposal[] getProposals(String contents, int position) {
		if (filterProposals) {
			StringMatcher matcher = new StringMatcher(contents, true, false);
			ArrayList<IContentProposal> list = new ArrayList<>();
			for (int i = 0; i < proposals.length; i++) {
				if (matcher.match(proposals[i])) {
					list.add(new ContentProposal(proposals[i]));
				}
			}
			return list.toArray(new IContentProposal[list.size()]);
		}
		if (contentProposals == null) {
			contentProposals = new IContentProposal[proposals.length];
			for (int i = 0; i < proposals.length; i++) {
				contentProposals[i] = new ContentProposal(proposals[i]);
			}
		}
		return contentProposals;
	}

	/**
	 * Set the Strings to be used as content proposals.
	 *
	 * @param items
	 *            the array of Strings to be used as proposals.
	 */
	public void setProposals(String[] items) {
		this.proposals = items;
		contentProposals = null;
	}

	/**
	 * Set the boolean that controls whether proposals are filtered according to
	 * the current field content.
	 *
	 * @param filterProposals
	 *            <code>true</code> if the proposals should be filtered to
	 *            show only those that match the current contents of the field,
	 *            and <code>false</code> if the proposals should remain the
	 *            same, ignoring the field content.
	 * @since 3.3
	 */
	public void setFiltering(boolean filterProposals) {
		this.filterProposals = filterProposals;
		// Clear any cached proposals.
		contentProposals = null;
	}
}
