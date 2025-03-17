package com.yhc.aospxpert.ui.models;

import androidx.annotation.IdRes;
import androidx.annotation.StringRes;
import androidx.annotation.XmlRes;

public class SearchPreferenceItem {

	private final @XmlRes int xml;
	private final @StringRes int title;
	private final @IdRes int actionId;

	public SearchPreferenceItem(int xml, int title, @IdRes int actionId) {
		this.xml = xml;
		this.title = title;
		this.actionId = actionId;
	}

	public int getXml() {
		return xml;
	}

	public int getTitle() {
		return title;
	}

	public int getActionId() {
		return actionId;
	}
}
