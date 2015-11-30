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
import com.guidebee.game.graphics.Color;
import com.guidebee.game.ui.drawable.Drawable;
import com.guidebee.utils.collections.Array;

//[------------------------------ MAIN CLASS ----------------------------------]

/**
 * A tree widget where each node has an icon, component, and child nodes.
 * <p/>
 * The preferred size of the tree is determined by the preferred size of the
 * components for the expanded nodes.
 * <p/>
 * {@link com.guidebee.game.ui.ChangeListener.ChangeEvent} is
 * fired when the selected node changes.
 *
 * @author Nathan Sweet
 */
public class Tree extends WidgetGroup {
    TreeStyle style;
    final Array<Node> rootNodes = new Array();
    final Selection<Node> selection;
    float ySpacing = 4, iconSpacingLeft = 2, iconSpacingRight = 2, padding = 0,
            indentSpacing;
    private float leftColumnWidth, prefWidth, prefHeight;
    private boolean sizeInvalid = true;
    private Node foundNode;
    Node overNode;
    private ClickListener clickListener;

    public Tree(Skin skin) {
        this(skin.get(TreeStyle.class));
    }

    public Tree(Skin skin, String styleName) {
        this(skin.get(styleName, TreeStyle.class));
    }

    public Tree(TreeStyle style) {
        selection = new Selection();
        selection.setComponent(this);
        selection.setMultiple(true);
        setStyle(style);
        initialize();
    }

    private void initialize() {
        addListener(clickListener = new ClickListener() {
            public void clicked(InputEvent event, float x, float y) {
                Node node = getNodeAt(y);
                if (node == null) return;
                if (node != getNodeAt(getTouchDownY())) return;
                if (selection.getMultiple() && selection.hasItems() && Utils.shift()) {
                    // Select range (shift).
                    float low = selection.getLastSelected().component.getY();
                    float high = node.component.getY();
                    if (!Utils.ctrl()) selection.clear();
                    if (low > high)
                        selectNodes(rootNodes, high, low);
                    else
                        selectNodes(rootNodes, low, high);
                    selection.fireChangeEvent();
                    return;
                }
                if (node.children.size > 0 && (!selection.getMultiple() || !Utils.ctrl())) {
                    // Toggle expanded.
                    float rowX = node.component.getX();
                    if (node.icon != null) rowX -= iconSpacingRight + node.icon.getMinWidth();
                    if (x < rowX) {
                        node.setExpanded(!node.expanded);
                        return;
                    }
                }
                if (!node.isSelectable()) return;
                selection.choose(node);
            }

            public boolean mouseMoved(InputEvent event, float x, float y) {
                setOverNode(getNodeAt(y));
                return false;
            }

            public void exit(InputEvent event, float x, float y, int pointer, UIComponent toComponent) {
                super.exit(event, x, y, pointer, toComponent);
                if (toComponent == null || !toComponent.isDescendantOf(Tree.this)) setOverNode(null);
            }
        });
    }

    public void setStyle(TreeStyle style) {
        this.style = style;
        indentSpacing = Math.max(style.plus.getMinWidth(), style.minus.getMinWidth()) + iconSpacingLeft;
    }

    public void add(Node node) {
        insert(rootNodes.size, node);
    }

    public void insert(int index, Node node) {
        remove(node);
        node.parent = null;
        rootNodes.insert(index, node);
        node.addToTree(this);
        invalidateHierarchy();
    }

    public void remove(Node node) {
        if (node.parent != null) {
            node.parent.remove(node);
            return;
        }
        rootNodes.removeValue(node, true);
        node.removeFromTree(this);
        invalidateHierarchy();
    }

    /**
     * Removes all tree nodes.
     */
    public void clearChildren() {
        super.clearChildren();
        setOverNode(null);
        rootNodes.clear();
        selection.clear();
    }

    public Array<Node> getNodes() {
        return rootNodes;
    }

    public void invalidate() {
        super.invalidate();
        sizeInvalid = true;
    }

    private void computeSize() {
        sizeInvalid = false;
        prefWidth = style.plus.getMinWidth();
        prefWidth = Math.max(prefWidth, style.minus.getMinWidth());
        prefHeight = getHeight();
        leftColumnWidth = 0;
        computeSize(rootNodes, indentSpacing);
        leftColumnWidth += iconSpacingLeft + padding;
        prefWidth += leftColumnWidth + padding;
        prefHeight = getHeight() - prefHeight;
    }

    private void computeSize(Array<Node> nodes, float indent) {
        float ySpacing = this.ySpacing;
        float spacing = iconSpacingLeft + iconSpacingRight;
        for (int i = 0, n = nodes.size; i < n; i++) {
            Node node = nodes.get(i);
            float rowWidth = indent + iconSpacingRight;
            UIComponent component = node.component;
            if (component instanceof Layout) {
                Layout layout = (Layout) component;
                rowWidth += layout.getPrefWidth();
                node.height = layout.getPrefHeight();
                layout.pack();
            } else {
                rowWidth += component.getWidth();
                node.height = component.getHeight();
            }
            if (node.icon != null) {
                rowWidth += spacing + node.icon.getMinWidth();
                node.height = Math.max(node.height, node.icon.getMinHeight());
            }
            prefWidth = Math.max(prefWidth, rowWidth);
            prefHeight -= node.height + ySpacing;
            if (node.expanded) computeSize(node.children, indent + indentSpacing);
        }
    }

    public void layout() {
        if (sizeInvalid) computeSize();
        layout(rootNodes, leftColumnWidth + indentSpacing + iconSpacingRight,
                getHeight() - ySpacing / 2);
    }

    private float layout(Array<Node> nodes, float indent, float y) {
        float ySpacing = this.ySpacing;
        for (int i = 0, n = nodes.size; i < n; i++) {
            Node node = nodes.get(i);
            UIComponent component = node.component;
            float x = indent;
            if (node.icon != null) x += node.icon.getMinWidth();
            y -= node.height;
            node.component.setPosition(x, y);
            y -= ySpacing;
            if (node.expanded) y = layout(node.children, indent + indentSpacing, y);
        }
        return y;
    }

    public void draw(Batch batch, float parentAlpha) {
        Color color = getColor();
        batch.setColor(color.r, color.g, color.b, color.a * parentAlpha);
        if (style.background != null) style.background.draw(batch, getX(), getY(),
                getWidth(), getHeight());
        draw(batch, rootNodes, leftColumnWidth);
        super.draw(batch, parentAlpha); // Draw components.
    }

    /**
     * Draws selection, icons, and expand icons.
     */
    private void draw(Batch batch, Array<Node> nodes, float indent) {
        Drawable plus = style.plus, minus = style.minus;
        float x = getX(), y = getY();
        for (int i = 0, n = nodes.size; i < n; i++) {
            Node node = nodes.get(i);
            UIComponent component = node.component;

            if (selection.contains(node) && style.selection != null) {
                style.selection.draw(batch, x, y + component.getY() - ySpacing / 2,
                        getWidth(), node.height + ySpacing);
            } else if (node == overNode && style.over != null) {
                style.over.draw(batch, x, y + component.getY() - ySpacing / 2,
                        getWidth(), node.height + ySpacing);
            }

            if (node.icon != null) {
                float iconY = component.getY()
                        + Math.round((node.height - node.icon.getMinHeight()) / 2);
                batch.setColor(component.getColor());
                node.icon.draw(batch, x + node.component.getX() - iconSpacingRight
                                - node.icon.getMinWidth(), y + iconY,
                        node.icon.getMinWidth(), node.icon.getMinHeight());
                batch.setColor(Color.WHITE);
            }

            if (node.children.size == 0) continue;

            Drawable expandIcon = node.expanded ? minus : plus;
            float iconY = component.getY()
                    + Math.round((node.height - expandIcon.getMinHeight()) / 2);
            expandIcon.draw(batch, x + indent - iconSpacingLeft,
                    y + iconY, expandIcon.getMinWidth(), expandIcon.getMinHeight());
            if (node.expanded) draw(batch, node.children,
                    indent + indentSpacing);
        }
    }

    /**
     * @return May be null.
     */
    public Node getNodeAt(float y) {
        foundNode = null;
        getNodeAt(rootNodes, y, getHeight());
        return foundNode;
    }

    private float getNodeAt(Array<Node> nodes, float y, float rowY) {
        for (int i = 0, n = nodes.size; i < n; i++) {
            Node node = nodes.get(i);
            if (y >= rowY - node.height - ySpacing && y < rowY) {
                foundNode = node;
                return -1;
            }
            rowY -= node.height + ySpacing;
            if (node.expanded) {
                rowY = getNodeAt(node.children, y, rowY);
                if (rowY == -1) return -1;
            }
        }
        return rowY;
    }

    void selectNodes(Array<Node> nodes, float low, float high) {
        for (int i = 0, n = nodes.size; i < n; i++) {
            Node node = nodes.get(i);
            if (node.component.getY() < low) break;
            if (!node.isSelectable()) continue;
            if (node.component.getY() <= high) selection.add(node);
            if (node.expanded) selectNodes(node.children, low, high);
        }
    }

    public Selection<Node> getSelection() {
        return selection;
    }

    public TreeStyle getStyle() {
        return style;
    }

    public Array<Node> getRootNodes() {
        return rootNodes;
    }

    public Node getOverNode() {
        return overNode;
    }

    public void setOverNode(Node overNode) {
        this.overNode = overNode;
    }

    /**
     * Sets the amount of horizontal space between the nodes and the
     * left/right edges of the tree.
     */
    public void setPadding(float padding) {
        this.padding = padding;
    }

    /**
     * Sets the amount of vertical space between nodes.
     */
    public void setYSpacing(float ySpacing) {
        this.ySpacing = ySpacing;
    }

    /**
     * Sets the amount of horizontal space between the node components and icons.
     */
    public void setIconSpacing(float left, float right) {
        this.iconSpacingLeft = left;
        this.iconSpacingRight = right;
    }

    public float getPrefWidth() {
        if (sizeInvalid) computeSize();
        return prefWidth;
    }

    public float getPrefHeight() {
        if (sizeInvalid) computeSize();
        return prefHeight;
    }

    public void findExpandedObjects(Array objects) {
        findExpandedObjects(rootNodes, objects);
    }

    public void restoreExpandedObjects(Array objects) {
        for (int i = 0, n = objects.size; i < n; i++) {
            Node node = findNode(objects.get(i));
            if (node != null) {
                node.setExpanded(true);
                node.expandTo();
            }
        }
    }

    static boolean findExpandedObjects(Array<Node> nodes, Array objects) {
        boolean expanded = false;
        for (int i = 0, n = nodes.size; i < n; i++) {
            Node node = nodes.get(i);
            if (node.expanded
                    && !findExpandedObjects(node.children, objects))
                objects.add(node.object);
        }
        return expanded;
    }

    /**
     * Returns the node with the specified object, or null.
     */
    public Node findNode(Object object) {
        if (object == null)
            throw new IllegalArgumentException("object cannot be null.");
        return findNode(rootNodes, object);
    }

    static Node findNode(Array<Node> nodes, Object object) {
        for (int i = 0, n = nodes.size; i < n; i++) {
            Node node = nodes.get(i);
            if (object.equals(node.object)) return node;
        }
        for (int i = 0, n = nodes.size; i < n; i++) {
            Node node = nodes.get(i);
            Node found = findNode(node.children, object);
            if (found != null) return found;
        }
        return null;
    }

    public void collapseAll() {
        collapseAll(rootNodes);
    }

    static void collapseAll(Array<Node> nodes) {
        for (int i = 0, n = nodes.size; i < n; i++) {
            Node node = nodes.get(i);
            node.setExpanded(false);
            collapseAll(node.children);
        }
    }

    public void expandAll() {
        expandAll(rootNodes);
    }

    static void expandAll(Array<Node> nodes) {
        for (int i = 0, n = nodes.size; i < n; i++)
            nodes.get(i).expandAll();
    }

    /**
     * Returns the click listener the tree uses for clicking on
     * nodes and the over node.
     */
    public ClickListener getClickListener() {
        return clickListener;
    }

    /**
     * Tree node.
     */
    static public class Node {
        UIComponent component;
        Node parent;
        final Array<Node> children = new Array(0);
        boolean selectable = true;
        boolean expanded;
        Drawable icon;
        float height;
        Object object;

        public Node(UIComponent component) {
            if (component == null)
                throw new IllegalArgumentException("component cannot be null.");
            this.component = component;
        }

        public void setExpanded(boolean expanded) {
            if (expanded == this.expanded) return;
            this.expanded = expanded;
            if (children.size == 0) return;
            Tree tree = getTree();
            if (tree == null) return;
            if (expanded) {
                for (int i = 0, n = children.size; i < n; i++)
                    children.get(i).addToTree(tree);
            } else {
                for (int i = 0, n = children.size; i < n; i++)
                    children.get(i).removeFromTree(tree);
            }
            tree.invalidateHierarchy();
        }

        /**
         * Called to add the component to the tree when the node's parent is expanded.
         */
        protected void addToTree(Tree tree) {
            tree.addComponent(component);
            if (!expanded) return;
            for (int i = 0, n = children.size; i < n; i++)
                children.get(i).addToTree(tree);
        }

        /**
         * Called to remove the component from the tree when the node's parent is collapsed.
         */
        protected void removeFromTree(Tree tree) {
            tree.removeComponent(component);
            if (!expanded) return;
            for (int i = 0, n = children.size; i < n; i++)
                children.get(i).removeFromTree(tree);
        }

        public void add(Node node) {
            insert(children.size, node);
        }

        public void addAll(Array<Node> nodes) {
            for (int i = 0, n = nodes.size; i < n; i++)
                insert(children.size, nodes.get(i));
        }

        public void insert(int index, Node node) {
            node.parent = this;
            children.insert(index, node);
            updateChildren();
        }

        public void remove() {
            Tree tree = getTree();
            if (tree != null)
                tree.remove(this);
            else if (parent != null) //
                parent.remove(this);
        }

        public void remove(Node node) {
            children.removeValue(node, true);
            if (!expanded) return;
            Tree tree = getTree();
            if (tree == null) return;
            node.removeFromTree(tree);
            if (children.size == 0) expanded = false;
        }

        public void removeAll() {
            Tree tree = getTree();
            if (tree != null) {
                for (int i = 0, n = children.size; i < n; i++)
                    children.get(i).removeFromTree(tree);
            }
            children.clear();
        }

        /**
         * Returns the tree this node is currently in, or null.
         */
        public Tree getTree() {
            UIContainer parent = component.getParent();
            if (!(parent instanceof Tree)) return null;
            return (Tree) parent;
        }

        public UIComponent getComponent() {
            return component;
        }

        public boolean isExpanded() {
            return expanded;
        }

        /**
         * If the children order is changed, {@link #updateChildren()} must be called.
         */
        public Array<Node> getChildren() {
            return children;
        }

        public void updateChildren() {
            if (!expanded) return;
            Tree tree = getTree();
            if (tree == null) return;
            for (int i = 0, n = children.size; i < n; i++)
                children.get(i).addToTree(tree);
        }

        /**
         * @return May be null.
         */
        public Node getParent() {
            return parent;
        }

        /**
         * Sets an icon that will be drawn to the left of the component.
         */
        public void setIcon(Drawable icon) {
            this.icon = icon;
        }

        public Object getObject() {
            return object;
        }

        /**
         * Sets an application specific object for this node.
         */
        public void setObject(Object object) {
            this.object = object;
        }

        public Drawable getIcon() {
            return icon;
        }

        /**
         * Returns this node or the child node with the specified object, or null.
         */
        public Node findNode(Object object) {
            if (object == null) throw new IllegalArgumentException("object cannot be null.");
            if (object.equals(this.object)) return this;
            return Tree.findNode(children, object);
        }

        /**
         * Collapses all nodes under and including this node.
         */
        public void collapseAll() {
            setExpanded(false);
            Tree.collapseAll(children);
        }

        /**
         * Expands all nodes under and including this node.
         */
        public void expandAll() {
            setExpanded(true);
            if (children.size > 0) Tree.expandAll(children);
        }

        /**
         * Expands all parent nodes of this node.
         */
        public void expandTo() {
            Node node = parent;
            while (node != null) {
                node.setExpanded(true);
                node = node.parent;
            }
        }

        public boolean isSelectable() {
            return selectable;
        }

        public void setSelectable(boolean selectable) {
            this.selectable = selectable;
        }

        public void findExpandedObjects(Array objects) {
            if (expanded && !Tree.findExpandedObjects(children, objects))
                objects.add(object);
        }

        public void restoreExpandedObjects(Array objects) {
            for (int i = 0, n = objects.size; i < n; i++) {
                Node node = findNode(objects.get(i));
                if (node != null) {
                    node.setExpanded(true);
                    node.expandTo();
                }
            }
        }
    }

    /**
     * The style for a {@link Tree}.
     *
     * @author Nathan Sweet
     */
    static public class TreeStyle {
        public Drawable plus, minus;
        /**
         * Optional.
         */
        public Drawable over, selection, background;

        public TreeStyle() {
        }

        public TreeStyle(Drawable plus, Drawable minus, Drawable selection) {
            this.plus = plus;
            this.minus = minus;
            this.selection = selection;
        }

        public TreeStyle(TreeStyle style) {
            this.plus = style.plus;
            this.minus = style.minus;
            this.selection = style.selection;
        }
    }
}
