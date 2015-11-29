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
package com.guidebee.game.scene.actions;

//--------------------------------- IMPORTS ------------------------------------
import com.guidebee.game.ui.UIComponent;

//[------------------------------ MAIN CLASS ----------------------------------]
/**
 * Removes an actor from the stage.
 *
 * @author Nathan Sweet
 */
public class RemoveActorAction extends Action {
    private UIComponent removeActor;
    private boolean removed;

    public boolean act(float delta) {
        if (!removed) {
            removed = true;
            UIComponent toRemove=(removeActor != null ? removeActor : actor);
            Object userObject=toRemove.getUserObject();
            if(userObject instanceof com.guidebee.game.scene.Actor){
                ((com.guidebee.game.scene.Actor)userObject).remove();
            }else{
                toRemove.remove();
            }

        }
        return true;
    }

    public void restart() {
        removed = false;
    }

    public void reset() {
        super.reset();
        removeActor = null;
    }

    public UIComponent getRemoveActor() {
        return removeActor;
    }

    /**
     * Sets the actor to remove. If null (the default),
     * the {@link #getActor() actor} will be used.
     */
    public void setRemoveActor(UIComponent removeActor) {
        this.removeActor = removeActor;
    }
}
