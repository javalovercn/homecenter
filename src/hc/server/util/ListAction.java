package hc.server.util;

public class ListAction {
	protected final String name;

	public String getDisplayName() {
		return name;
	}

	public String getName() {
		return name;
	}

	public ListAction(final String name) {
		this.name = name;
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof ListAction) {
			return name.equals(((ListAction) obj).name);
		}
		return false;
	}
}
