package com.combatcore.gui;

import com.combatcore.CombatCoreMod;
import com.combatcore.config.ConfigManager;
import com.combatcore.module.Module;
import com.combatcore.module.setting.NumberSetting;
import com.combatcore.module.setting.Setting;
import com.combatcore.util.Keys;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Basic ClickGUI, opened with Right Shift (a vanilla key binding, see
 * {@link com.combatcore.CombatCoreMod#GUI_KEY}; its code is also stored as
 * {@code guiKey} in the config JSON). One column, grouped by module category:
 *
 * <ul>
 *   <li>left click a module - toggle it,</li>
 *   <li>right click a module - expand/collapse its settings,</li>
 *   <li>left click a slider - drag to change the value,</li>
 *   <li>left click the key row - press a key to bind, ESC/Delete clears,</li>
 *   <li>ESC or Right Shift - close (config is saved on close).</li>
 * </ul>
 *
 * <p>Rendering uses only vanilla primitives ({@code fill} and the text
 * renderer), no GL state is left behind, and the layout is rebuilt from the
 * module list on demand, so it stays correct on resize and after any change.
 */
public class ClickGuiScreen extends Screen {

	private static final int PANEL_WIDTH = 150;
	private static final int PADDING = 4;
	private static final int TRACK_MARGIN = 5;

	private static final int COL_DIM = 0x90000000;
	private static final int COL_BORDER = 0xFF000000;
	private static final int COL_PANEL = 0xF014141C;
	private static final int COL_HEADER_BG = 0xE01D1D28;
	private static final int COL_HEADER_TEXT = 0xFF55D5FF;
	private static final int COL_HOVER = 0x28FFFFFF;
	private static final int COL_TEXT = 0xFFE8E8EE;
	private static final int COL_TEXT_DIM = 0xFF8A8A96;
	private static final int COL_ON = 0xFF55FF55;
	private static final int COL_OFF = 0xFF5A5A66;
	private static final int COL_TRACK = 0xFF26262F;
	private static final int COL_SLIDER = 0xFF00AAFF;

	private static final int TYPE_HEADER = 0;
	private static final int TYPE_MODULE = 1;
	private static final int TYPE_SETTING = 2;
	private static final int TYPE_KEY = 3;

	private final List<Row> rows = new ArrayList<Row>();

	private int panelX;
	private int panelY;
	private int panelHeight;

	private Module expanded;
	private Module bindingModule;
	private NumberSetting dragging;

	private static final class Row {
		private final int type;
		private final Module module;
		private final Setting<?> setting;
		private final String label;
		private final int x;
		private int y;
		private final int width;
		private final int height;

		private Row(int type, Module module, Setting<?> setting, String label, int x, int y, int width, int height) {
			this.type = type;
			this.module = module;
			this.setting = setting;
			this.label = label;
			this.x = x;
			this.y = y;
			this.width = width;
			this.height = height;
		}

		private boolean contains(int mouseX, int mouseY) {
			return mouseX >= this.x && mouseX < this.x + this.width
					&& mouseY >= this.y && mouseY < this.y + this.height;
		}
	}

	@Override
	public void render(int mouseX, int mouseY, float delta) {
		this.buildRows();

		super.render(mouseX, mouseY, delta);

		fill(0, 0, this.width, this.height, COL_DIM);
		fill(this.panelX - 2, this.panelY - 2,
				this.panelX + PANEL_WIDTH + 2, this.panelY + this.panelHeight + 2, COL_BORDER);
		fill(this.panelX, this.panelY,
				this.panelX + PANEL_WIDTH, this.panelY + this.panelHeight, COL_PANEL);

		for (Row row : this.rows) {
			this.drawRow(row, mouseX, mouseY);
		}

		String hint = this.bindingModule != null
				? "Press a key - ESC / Delete clears"
				: "Left click: toggle - Right click: settings";
		this.textRenderer.drawWithShadow(hint,
				(this.width - this.textRenderer.getWidth(hint)) / 2.0F,
				this.panelY + this.panelHeight + 6, COL_TEXT_DIM);
	}

	private void drawRow(Row row, int mouseX, int mouseY) {
		boolean hover = row.contains(mouseX, mouseY);
		int textY = row.y + (row.height - this.textRenderer.fontHeight) / 2;

		switch (row.type) {
			case TYPE_HEADER: {
				if (hover) {
					fill(row.x, row.y, row.x + row.width, row.y + row.height, COL_HEADER_BG);
				}
				this.textRenderer.drawWithShadow(row.label.toUpperCase(),
						row.x + 4, textY, COL_HEADER_TEXT);
				break;
			}

			case TYPE_MODULE: {
				if (hover) {
					fill(row.x, row.y, row.x + row.width, row.y + row.height, COL_HOVER);
				}

				boolean on = row.module.isEnabled();
				this.textRenderer.drawWithShadow(row.module.getName(),
						row.x + 4, textY, on ? COL_ON : COL_TEXT);

				if (row.module.getKeyCode() > 0) {
					String key = Keys.getName(row.module.getKeyCode());
					this.textRenderer.draw(key,
							row.x + row.width - 16 - this.textRenderer.getWidth(key),
							textY, COL_TEXT_DIM);
				}

				int squareTop = row.y + (row.height - 7) / 2;
				fill(row.x + row.width - 11, squareTop,
						row.x + row.width - 4, squareTop + 7, on ? COL_ON : COL_OFF);
				break;
			}

			case TYPE_SETTING: {
				NumberSetting setting = (NumberSetting) row.setting;
				if (hover) {
					fill(row.x, row.y, row.x + row.width, row.y + row.height, COL_HOVER);
				}

				int trackX = row.x + TRACK_MARGIN;
				int trackWidth = row.width - TRACK_MARGIN * 2;
				int trackTop = row.y + row.height / 2 - 3;
				fill(trackX, trackTop, trackX + trackWidth, trackTop + 6, COL_TRACK);
				fill(trackX, trackTop, trackX + Math.round(trackWidth * setting.getFraction()),
						trackTop + 6, COL_SLIDER);

				this.textRenderer.drawWithShadow(setting.getDisplayString(),
						trackX + 2, textY, COL_TEXT);
				break;
			}

			case TYPE_KEY: {
				if (hover) {
					fill(row.x, row.y, row.x + row.width, row.y + row.height, COL_HOVER);
				}

				boolean waiting = this.bindingModule == row.module;
				String label = waiting
						? "> press a key <"
						: "Key: [" + Keys.getName(row.module.getKeyCode()) + "]";
				this.textRenderer.drawWithShadow(label, row.x + 4, textY,
						waiting ? COL_SLIDER : COL_TEXT);
				break;
			}

			default:
				break;
		}
	}

	/** Rebuilds the row layout (and panel geometry) from the current state. */
	private void buildRows() {
		this.rows.clear();

		int rowHeight = this.textRenderer.fontHeight + 5;
		int headerHeight = this.textRenderer.fontHeight + 7;
		int x = (this.width - PANEL_WIDTH) / 2;
		int y = 0;

		Set<String> categories = new LinkedHashSet<String>();
		for (Module module : CombatCoreMod.MODULES.getModules()) {
			categories.add(module.getCategory());
		}

		for (String category : categories) {
			this.rows.add(new Row(TYPE_HEADER, null, null, category, x, y, PANEL_WIDTH, headerHeight));
			y += headerHeight;

			for (Module module : CombatCoreMod.MODULES.getModules()) {
				if (!module.getCategory().equals(category)) {
					continue;
				}

				this.rows.add(new Row(TYPE_MODULE, module, null, null, x, y, PANEL_WIDTH, rowHeight));
				y += rowHeight;

				if (module != this.expanded) {
					continue;
				}

				for (Setting<?> setting : module.getSettings()) {
					this.rows.add(new Row(TYPE_SETTING, module, setting, null, x, y, PANEL_WIDTH, rowHeight));
					y += rowHeight;
				}

				this.rows.add(new Row(TYPE_KEY, module, null, null, x, y, PANEL_WIDTH, rowHeight));
				y += rowHeight;
			}
		}

		this.panelX = x;
		this.panelHeight = y + PADDING * 2;
		this.panelY = Math.max(10, (this.height - this.panelHeight) / 2);

		int offsetY = this.panelY + PADDING;
		for (Row row : this.rows) {
			row.y += offsetY;
		}
	}

	private Row findSettingRow(NumberSetting setting) {
		for (Row row : this.rows) {
			if (row.type == TYPE_SETTING && row.setting == setting) {
				return row;
			}
		}

		return null;
	}

	private void updateSlider(Row row, int mouseX) {
		NumberSetting setting = (NumberSetting) row.setting;
		float fraction = (mouseX - (row.x + TRACK_MARGIN))
				/ (float) Math.max(1, row.width - TRACK_MARGIN * 2);

		setting.setValue(setting.valueAtFraction(fraction));
		ConfigManager.markDirty();
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int button) {
		this.buildRows();

		for (Row row : this.rows) {
			if (!row.contains(mouseX, mouseY)) {
				continue;
			}

			switch (row.type) {
				case TYPE_MODULE:
					if (button == 0) {
						row.module.toggle();
						ConfigManager.markDirty();
					} else if (button == 1) {
						this.expanded = this.expanded == row.module ? null : row.module;
						this.bindingModule = null;
					}
					return;

				case TYPE_SETTING:
					if (button == 0 && row.setting instanceof NumberSetting) {
						this.dragging = (NumberSetting) row.setting;
						this.updateSlider(row, mouseX);
					}
					return;

				case TYPE_KEY:
					if (button == 0) {
						this.bindingModule = row.module;
					}
					return;

				default:
					return;
			}
		}
	}

	@Override
	protected void mouseDragged(int mouseX, int mouseY, int button, long dragTime) {
		if (this.dragging != null) {
			this.buildRows();
			Row row = this.findSettingRow(this.dragging);
			if (row != null) {
				this.updateSlider(row, mouseX);
			}
		} else {
			super.mouseDragged(mouseX, mouseY, button, dragTime);
		}
	}

	@Override
	protected void mouseReleased(int mouseX, int mouseY, int button) {
		if (this.dragging != null) {
			this.dragging = null;
			ConfigManager.markDirty();
		} else {
			super.mouseReleased(mouseX, mouseY, button);
		}
	}

	@Override
	protected void keyPressed(char typedChar, int keyCode) {
		if (this.bindingModule != null) {
			if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_BACK
					|| keyCode == Keyboard.KEY_DELETE) {
				this.bindingModule.setKeyCode(0);
			} else if (keyCode > 0) {
				this.bindingModule.setKeyCode(keyCode);
			} else {
				return;
			}

			this.bindingModule = null;
			ConfigManager.markDirty();
			return;
		}

		if (keyCode == CombatCoreMod.GUI_KEY.getKeyCode()) {
			// Suppress the next open: key-repeat events of the same physical
			// press must not reopen the GUI right after this close.
			CombatCoreMod.suppressGuiOpen();
			this.minecraft.openScreen(null);
			return;
		}

		// Vanilla handles ESC (close + re-grab the mouse).
		super.keyPressed(typedChar, keyCode);
	}

	@Override
	public void removed() {
		ConfigManager.saveNow();
	}

	@Override
	public boolean shouldPauseGame() {
		return false;
	}
}
