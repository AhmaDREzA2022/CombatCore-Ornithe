package com.combatcore.module.setting;

/**
 * Base class for a module setting: a named, sanitised value that is rendered
 * as a control in the ClickGUI and persisted by the config system.
 *
 * <p>Subclasses decide how invalid input is coerced back into range
 * ({@link #sanitize(Object)}), so callers can blindly call
 * {@link #setValue(Object)} with anything (config JSON, slider maths, ...).
 */
public abstract class Setting<T> {

	private final String name;
	private T value;

	protected Setting(String name, T value) {
		this.name = name;
		// Deliberately NOT sanitize() here: a virtual call from the constructor
		// would run before subclass fields (min/max) are assigned and clamp the
		// default against zero-initialized bounds (NumberSetting would end up
		// with 0 for every setting). Constructors must pass an already-valid
		// value; setValue() sanitises all later input once fields exist.
		this.value = value;
	}

	public String getName() {
		return this.name;
	}

	public T getValue() {
		return this.value;
	}

	public void setValue(T value) {
		this.value = sanitize(value);
	}

	/**
	 * Coerce {@code value} into a valid state. Called on every
	 * {@link #setValue(Object)} - never from the constructor, where subclass
	 * fields (min/max) are not assigned yet.
	 */
	protected T sanitize(T value) {
		return value;
	}
}
