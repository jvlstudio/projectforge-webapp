/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2013 Kai Reinhard (k.reinhard@micromata.de)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.web.wicket.flowlayout;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;

import de.micromata.wicket.ajax.behavior.JavaScriptEventToggleBehavior;
import de.micromata.wicket.ajax.behavior.ToggleStatus;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class ToggleContainerPanel extends Panel
{
  private static final long serialVersionUID = 6130552547273354134L;

  public static final String CONTENT_ID = "content";

  public static final String HEADING_ID = "heading";

  private final WebMarkupContainer panel, toggleContainer, toggleLink;

  /**
   * @param id
   */
  public ToggleContainerPanel(final String id, final DivType... cssClasses)
  {
    super(id);
    panel = new WebMarkupContainer("panel");
    panel.setOutputMarkupId(true);
    super.add(panel);
    if (cssClasses != null) {
      for (final DivType cssClass : cssClasses) {
        panel.add(AttributeModifier.append("class", cssClass.getClassAttrValue()));
      }
    }
    panel.add(toggleContainer = new WebMarkupContainer("toggleContainer"));
    toggleContainer.setOutputMarkupId(true);
    panel.add(toggleLink = new WebMarkupContainer("toggleLink"));
    if (wantsOnStatusChangedNotification()) {
      toggleLink.add(new JavaScriptEventToggleBehavior() {
        private static final long serialVersionUID = -3739318529449433236L;

        @Override
        protected void onToggleCall(final AjaxRequestTarget target, final ToggleStatus toggleStatus)
        {
          ToggleContainerPanel.this.onToggleStatusChanged(target, toggleStatus);
        }
      });
    }
    setOpen();
  }

  public ToggleContainerPanel setHeading(final String heading)
  {
    toggleLink.add(new Label(HEADING_ID, heading).setRenderBodyOnly(true));
    return this;
  }

  /**
   * @param heading Must have the component id {@link #HEADING_ID}.
   * @return
   */
  public ToggleContainerPanel setHeading(final Component heading)
  {
    toggleLink.add(heading);
    return this;
  }

  @Override
  public ToggleContainerPanel setMarkupId(final String id)
  {
    toggleContainer.setMarkupId(id);
    return this;
  }

  public WebMarkupContainer getContainer()
  {
    return toggleContainer;
  }

  /**
   * Returns whether the subclass wants to be notified on toggle status change
   * 
   * @return
   */
  protected boolean wantsOnStatusChangedNotification()
  {
    return false;
  }

  /**
   * Hook method when the toggle status of this {@link ToggleContainerPanel} was changed.
   * 
   * @param target
   * @param toggleClosed this represents the <b>new</b> state of the toggle.
   */
  protected void onToggleStatusChanged(final AjaxRequestTarget target, final ToggleStatus toggleStatus)
  {

  }

  /**
   * @see org.apache.wicket.MarkupContainer#add(org.apache.wicket.Component[])
   */
  public MarkupContainer add(final DivPanel content)
  {
    return toggleContainer.add(content);
  }

  /**
   * Calls div.add(...);
   * @see org.apache.wicket.Component#add(org.apache.wicket.behavior.Behavior[])
   */
  @Override
  public Component add(final Behavior... behaviors)
  {
    return toggleContainer.add(behaviors);
  }

  /**
   * Has only effect before rendering this component the first time. Must be called after heading was set.
   * @return this for chaining.
   */
  public ToggleContainerPanel setOpen()
  {
    toggleContainer.add(AttributeModifier.replace("class", "in collapse"));
    return this;
  }

  /**
   * Has only effect before rendering this component the first time. Must be called after heading was set.
   * @return this for chaining.
   */
  public ToggleContainerPanel setClosed()
  {
    toggleContainer.add(AttributeModifier.replace("class", "collapse"));
    return this;
  }
}
