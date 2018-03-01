package hc.server.util.ai;

import java.util.Vector;

public class TotalScreenScore {
	Vector<ScreenScore> listScore;

	public final void addScreenScore(final String projectID, final String targetID,
			final MatchScore ms) {
		if (listScore == null) {
			listScore = new Vector<ScreenScore>(10);
		}

		final int size = listScore.size();
		for (int i = 0; i < size; i++) {
			final ScreenScore screenScore = listScore.get(i);
			if (screenScore.projectID.equals(projectID) && screenScore.target.equals(targetID)) {
				screenScore.addKey(ms.fromKey);
				screenScore.addMatchScore(ms);
				return;
			}
		}

		final ScreenScore ss = new ScreenScore(projectID, targetID);
		ss.addKey(ms.fromKey);
		ss.addMatchScore(ms);
		listScore.add(ss);
	}

	public final void addKeyOnly(final String projectID, final String targetID,
			final String fromKey) {
		if (listScore == null) {
			return;
		}

		if (targetID == LabelManager.PROJECT_TITLE) {
			// to all project
			final int size = listScore.size();
			for (int i = 0; i < size; i++) {
				final ScreenScore screenScore = listScore.get(i);
				if (screenScore.projectID.equals(projectID)) {
					screenScore.addKey(fromKey);
				}
			}
		} else {
			final int size = listScore.size();
			for (int i = 0; i < size; i++) {
				final ScreenScore screenScore = listScore.get(i);
				if (screenScore.projectID.equals(projectID)
						&& screenScore.target.equals(targetID)) {
					screenScore.addKey(fromKey);
					return;
				}
			}
		}
	}
}
