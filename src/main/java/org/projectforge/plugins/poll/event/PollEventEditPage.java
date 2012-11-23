/////////////////////////////////////////////////////////////////////////////
//
// Project   ProjectForge
//
// Copyright 2001-2009, Micromata GmbH, Kai Reinhard
//           All rights reserved.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.plugins.poll.event;

import net.ftlines.wicket.fullcalendar.CalendarResponse;
import net.ftlines.wicket.fullcalendar.EventSource;
import net.ftlines.wicket.fullcalendar.callback.ClickedEvent;
import net.ftlines.wicket.fullcalendar.callback.DroppedEvent;
import net.ftlines.wicket.fullcalendar.callback.ResizedEvent;
import net.ftlines.wicket.fullcalendar.callback.SelectedRange;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.projectforge.plugins.poll.PollDO;
import org.projectforge.web.calendar.MyFullCalendar;
import org.projectforge.web.calendar.MyFullCalendarConfig;
import org.projectforge.web.wicket.AbstractSecuredPage;

/**
 * @author Johannes Unterstein (j.unterstein@micromata.de)
 * 
 */
public class PollEventEditPage extends AbstractSecuredPage
{
  private static final long serialVersionUID = 2988767055605267801L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(PollEventEditPage.class);

  private MyFullCalendarConfig config;

  private MyFullCalendar calendar;

  private final IModel<PollDO> model;

  /**
   * @param parameters
   */
  public PollEventEditPage(final PageParameters parameters)
  {
    super(parameters);
    model = new Model<PollDO>(new PollDO());
  }

  /**
   * @see org.apache.wicket.Component#onInitialize()
   */
  @Override
  protected void onInitialize()
  {
    super.onInitialize();
    final PollEventEventsProvider eventProvider = new PollEventEventsProvider(this, model);
    config = new MyFullCalendarConfig(this);
    config.setSelectable(true);
    config.setEditable(true);
    config.setSelectHelper(true);
    config.setDefaultView("agendaWeek");
    config.getHeader().setRight("");
    config.setEnableContextMenu(false);
    config.setLoading("function(bool) { if (bool) $(\"#loading\").show(); else $(\"#loading\").hide(); }");
    calendar = new MyFullCalendar("cal", config) {
      private static final long serialVersionUID = -6819899072933690316L;

      /**
       * @see net.ftlines.wicket.fullcalendar.FullCalendar#onDateRangeSelected(net.ftlines.wicket.fullcalendar.callback.SelectedRange,
       *      net.ftlines.wicket.fullcalendar.CalendarResponse)
       */
      @Override
      protected void onDateRangeSelected(final SelectedRange range, final CalendarResponse response)
      {
        eventProvider.addEvent(range, response);
      }

      /**
       * @see net.ftlines.wicket.fullcalendar.FullCalendar#onEventResized(net.ftlines.wicket.fullcalendar.callback.ResizedEvent,
       *      net.ftlines.wicket.fullcalendar.CalendarResponse)
       */
      @Override
      protected boolean onEventResized(final ResizedEvent event, final CalendarResponse response)
      {
        return eventProvider.resizeEvent(event, response);
      }

      /**
       * @see net.ftlines.wicket.fullcalendar.FullCalendar#onEventDropped(net.ftlines.wicket.fullcalendar.callback.DroppedEvent,
       *      net.ftlines.wicket.fullcalendar.CalendarResponse)
       */
      @Override
      protected boolean onEventDropped(final DroppedEvent event, final CalendarResponse response)
      {
        return eventProvider.dropEvent(event, response);
      }
      /**
       * @see net.ftlines.wicket.fullcalendar.FullCalendar#onEventClicked(net.ftlines.wicket.fullcalendar.callback.ClickedEvent, net.ftlines.wicket.fullcalendar.CalendarResponse)
       */
      @Override
      protected void onEventClicked(final ClickedEvent event, final CalendarResponse response)
      {
        eventProvider.eventClicked(event, response);
      }
    };
    calendar.setMarkupId("calendar");
    final EventSource eventSource = new EventSource();
    eventSource.setEventsProvider(eventProvider);
    config.add(eventSource);
    body.add(calendar);
  }

  /**
   * @see org.projectforge.web.wicket.AbstractUnsecureBasePage#getTitle()
   */
  @Override
  protected String getTitle()
  {
    return getString("plugins.poll.title");
  }

}