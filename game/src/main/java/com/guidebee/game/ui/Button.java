/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
//--------------------------------- PACKAGE ------------------------------------
package com.guidebee.game.ui;

//--------------------------------- IMPORTS ------------------------------------

import com.guidebee.game.graphics.Batch;
import com.guidebee.game.ui.drawable.Drawable;
import com.guidebee.utils.Pools;
import com.guidebee.utils.collections.Array;

//[------------------------------ MAIN CLASS ----------------------------------]

/**
 * A button is a {@link Table} with a checked state and additional
 * {@link ButtonStyle style} fields for pressed, unpressed, and
 * checked. Each time a button is clicked, the checked state is toggled.
 * Being a table, a button can contain any other components.
 * <p/>
 * {@link com.guidebee.game.ui.ChangeListener.ChangeEvent}
 * is fired when the button is clicked. Cancelling the event will restore
 * the checked button state to what is
 * was previously.
 * <p/>
 * The preferred size of the button is determined by the background and
 * the button contents.
 *
 * @author Nathan Sweet
 */
public class Button extends Table implements Disableable {
    private ButtonStyle style;
    boolean isChecked, isDisabled;
    ButtonGroup buttonGroup;
    private ClickListener clickListener;

    public Button(Skin skin) {
        super(skin);
        initialize();
        setStyle(skin.get(ButtonStyle.class));
        setSize(getPrefWidth(), getPrefHeight());
    }

    public Button(Skin skin, String styleName) {
        super(skin);
        initialize();
        setStyle(skin.get(styleName, ButtonStyle.class));
        setSize(getPrefWidth(), getPrefHeight());
    }

    public Button(UIComponent child, Skin skin, String styleName) {
        this(child, skin.get(styleName, ButtonStyle.class));
    }

    public Button(UIComponent child, ButtonStyle style) {
        initialize();
        add(child);
        setStyle(style);
        setSize(getPrefWidth(), getPrefHeight());
    }

    public Button(ButtonStyle style) {
        initialize();
        setStyle(style);
        setSize(getPrefWidth(), getPrefHeight());
    }

    /**
     * Creates a button without setting the style or size. At least a style
     * must be set before using this button.
     */
    public Button() {
        initialize();
    }

    private void initialize() {
        setTouchable(Touchable.enabled);
        addListener(clickListener = new ClickListener() {
            public void clicked(InputEvent event, float x, float y) {
                if (isDisabled) return;
                setChecked(!isChecked);
            }
        });
    }

    public Button(Drawable up) {
        this(new ButtonStyle(up, null, null));
    }

    public Button(Drawable up, Drawable down) {
        this(new ButtonStyle(up, down, null));
    }

    public Button(Drawable up, Drawable down, Drawable checked) {
        this(new ButtonStyle(up, down, checked));
    }

    public Button(UIComponent child, Skin skin) {
        this(child, skin.get(ButtonStyle.class));
    }

    public void setChecked(boolean isChecked) {
        if (this.isChecked == isChecked) return;
        if (buttonGroup != null && !buttonGroup.canCheck(this, isChecked)) return;
        this.isChecked = isChecked;
        ChangeListener.ChangeEvent changeEvent = Pools.obtain(ChangeListener.ChangeEvent.class);
        if (fire(changeEvent)) this.isChecked = !isChecked;
        Pools.free(changeEvent);
    }

    /**
     * Toggles the checked state. This method changes the checked state,
     * which fires a {@link com.guidebee.game.ui.ChangeListener.ChangeEvent},
     * so can be used to
     * simulate a button click.
     */
    public void toggle() {
        setChecked(!isChecked);
    }

    public boolean isChecked() {
        return isChecked;
    }

    public boolean isPressed() {
        return clickListener.isVisualPressed();
    }

    public boolean isOver() {
        return clickListener.isOver();
    }

    public ClickListener getClickListener() {
        return clickListener;
    }

    public boolean isDisabled() {
        return isDisabled;
    }

    /**
     * When true, the button will not toggle {@link #isChecked()} when clicked and
     * will not fire a {@link com.guidebee.game.ui.ChangeListener.ChangeEvent}.
     */
    public void setDisabled(boolean isDisabled) {
        this.isDisabled = isDisabled;
    }

    public void setStyle(ButtonStyle style) {
        if (style == null)
            throw new IllegalArgumentException("style cannot be null.");
        this.style = style;

        Drawable background = style.up;
        if (background == null) {
            background = style.down;
            if (background == null) background = style.checked;
        }
        if (background != null) {
            padBottom(background.getBottomHeight());
            padTop(background.getTopHeight());
            padLeft(background.getLeftWidth());
            padRight(background.getRightWidth());
        }
        invalidateHierarchy();
    }

    /**
     * Returns the button's style. Modifying the returned style may not have an
     * effect until {@link #setStyle(ButtonStyle)} is
     * called.
     */
    public ButtonStyle getStyle() {
        return style;
    }

    public void draw(Batch batch, float parentAlpha) {
        validate();

        Drawable background = null;
        float offsetX = 0, offsetY = 0;
        if (isPressed() && !isDisabled) {
            background = style.down == null ? style.up : style.down;
            offsetX = style.pressedOffsetX;
            offsetY = style.pressedOffsetY;
        } else {
            if (isDisabled && style.disabled != null)
                background = style.disabled;
            else if (isChecked && style.checked != null)
                background = (isOver() && style.checkedOver != null)
                        ? style.checkedOver : style.checked;
            else if (isOver() && style.over != null)
                background = style.over;
            else
                background = style.up;
            offsetX = style.unpressedOffsetX;
            offsetY = style.unpressedOffsetY;
        }
        setBackground(background, false);

        Array<UIComponent> children = getChildren();
        for (int i = 0; i < children.size; i++)
            children.get(i).moveBy(offsetX, offsetY);
        super.draw(batch, parentAlpha);
        for (int i = 0; i < children.size; i++)
            children.get(i).moveBy(-offsetX, -offsetY);
    }

    public float getPrefWidth() {
        float width = super.getPrefWidth();
        if (style.up != null) width = Math.max(width, style.up.getMinWidth());
        if (style.down != null) width = Math.max(width, style.down.getMinWidth());
        if (style.checked != null) width = Math.max(width, style.checked.getMinWidth());
        return width;
    }

    public float getPrefHeight() {
        float height = super.getPrefHeight();
        if (style.up != null) height = Math.max(height, style.up.getMinHeight());
        if (style.down != null) height = Math.max(height, style.down.getMinHeight());
        if (style.checked != null) height = Math.max(height, style.checked.getMinHeight());
        return height;
    }

    public float getMinWidth() {
        return getPrefWidth();
    }

    public float getMinHeight() {
        return getPrefHeight();
    }

    /**
     * The style for a button, see {@link Button}.
     *
     * @author mzechner
     */
    static public class ButtonStyle {
        /**
         * Optional.
         */
        public Drawable up, down, over, checked, checkedOver, disabled;
        /**
         * Optional.
         */
        public float pressedOffsetX, pressedOffsetY;
        /**
         * Optional.
         */
        public float unpressedOffsetX, unpressedOffsetY;

        public ButtonStyle() {
        }

        public ButtonStyle(Drawable up, Drawable down, Drawable checked) {
            this.up = up;
            this.down = down;
            this.checked = checked;
        }

        public ButtonStyle(ButtonStyle style) {
            this.up = style.up;
            this.down = style.down;
            this.over = style.over;
            this.checked = style.checked;
            this.checkedOver = style.checkedOver;
            this.disabled = style.disabled;
            this.pressedOffsetX = style.pressedOffsetX;
            this.pressedOffsetY = style.pressedOffsetY;
            this.unpressedOffsetX = style.unpressedOffsetX;
            this.unpressedOffsetY = style.unpressedOffsetY;
        }
    }
}
