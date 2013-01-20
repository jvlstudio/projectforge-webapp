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

package org.projectforge.web.task;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.validation.IFormValidator;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.convert.IConverter;
import org.hibernate.Hibernate;
import org.projectforge.common.StringHelper;
import org.projectforge.core.Priority;
import org.projectforge.fibu.ProjektDO;
import org.projectforge.fibu.kost.Kost2DO;
import org.projectforge.gantt.GanttObjectType;
import org.projectforge.gantt.GanttRelationType;
import org.projectforge.task.TaskDO;
import org.projectforge.task.TaskDao;
import org.projectforge.task.TaskStatus;
import org.projectforge.task.TaskTree;
import org.projectforge.task.TimesheetBookingStatus;
import org.projectforge.user.PFUserContext;
import org.projectforge.user.PFUserDO;
import org.projectforge.user.UserGroupCache;
import org.projectforge.web.fibu.Kost2ListPage;
import org.projectforge.web.fibu.Kost2SelectPanel;
import org.projectforge.web.user.UserSelectPanel;
import org.projectforge.web.wicket.AbstractEditForm;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.components.DatePanel;
import org.projectforge.web.wicket.components.DatePanelSettings;
import org.projectforge.web.wicket.components.LabelValueChoiceRenderer;
import org.projectforge.web.wicket.components.MaxLengthTextArea;
import org.projectforge.web.wicket.components.MaxLengthTextField;
import org.projectforge.web.wicket.components.MinMaxNumberField;
import org.projectforge.web.wicket.converter.IntegerPercentConverter;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.DivTextPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.InputPanel;
import org.projectforge.web.wicket.flowlayout.RadioGroupPanel;
import org.projectforge.web.wicket.flowlayout.TextAreaPanel;

public class TaskEditForm extends AbstractEditForm<TaskDO, TaskEditPage>
{
  private static final long serialVersionUID = -3784956996856970327L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(TaskEditForm.class);

  public static final BigDecimal MAX_DURATION_DAYS = new BigDecimal(10000);

  @SpringBean(name = "taskTree")
  private TaskTree taskTree;

  @SpringBean(name = "userGroupCache")
  private UserGroupCache userGroupCache;

  protected MaxLengthTextField kost2BlackWhiteTextField;

  @SuppressWarnings("unused")
  private Integer kost2Id;

  // Components for form validation.
  private final FormComponent< ? >[] dependentFormComponents = new FormComponent[2];

  public TaskEditForm(final TaskEditPage parentPage, final TaskDO data)
  {
    super(parentPage, data);
  }

  @SuppressWarnings("serial")
  @Override
  protected void init()
  {
    super.init();
    add(new IFormValidator() {
      @Override
      public FormComponent< ? >[] getDependentFormComponents()
      {
        return dependentFormComponents;
      }

      @SuppressWarnings("unchecked")
      @Override
      public void validate(final Form< ? > form)
      {
        final MinMaxNumberField<BigDecimal> durationField = (MinMaxNumberField<BigDecimal>) dependentFormComponents[0];
        final DatePanel endDate = (DatePanel) dependentFormComponents[1];
        if (durationField.getConvertedInput() != null && endDate.getDateField().getConvertedInput() != null) {
          error(getString("gantt.error.durationAndEndDateAreMutuallyExclusive"));
        }
      }
    });
    gridBuilder.newGridPanel();
    {
      // Parent task
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("task.parentTask"));
      final TaskSelectPanel parentTaskSelectPanel = new TaskSelectPanel(fs.newChildId(), new PropertyModel<TaskDO>(data, "parentTask"),
          parentPage, "parentTaskId");
      fs.add(parentTaskSelectPanel);
      parentTaskSelectPanel.init();
      if (taskTree.isRootNode(data) == false) {
        parentTaskSelectPanel.setRequired(true);
      } else {
        parentTaskSelectPanel.setVisible(false);
      }
      parentTaskSelectPanel.setRequired(true);
    }
    {
      // Title
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("task.title"));
      final MaxLengthTextField title = new MaxLengthTextField(InputPanel.WICKET_ID, new PropertyModel<String>(data, "title"));
      WicketUtils.setStrong(title);
      fs.add(title);
      if (isNew() == true) {
        WicketUtils.setFocus(title);
      }
    }
    gridBuilder.newSplitPanel(GridSize.COL50);
    {
      // Status drop down box:
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("status"));
      final LabelValueChoiceRenderer<TaskStatus> statusChoiceRenderer = new LabelValueChoiceRenderer<TaskStatus>(fs, TaskStatus.values());
      final DropDownChoice<TaskStatus> statusChoice = new DropDownChoice<TaskStatus>(fs.getDropDownChoiceId(),
          new PropertyModel<TaskStatus>(data, "status"), statusChoiceRenderer.getValues(), statusChoiceRenderer);
      statusChoice.setNullValid(false).setRequired(true);
      fs.add(statusChoice);
    }
    {
      // Assigned user:
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("task.assignedUser"));
      PFUserDO responsibleUser = data.getResponsibleUser();
      if (Hibernate.isInitialized(responsibleUser) == false) {
        responsibleUser = userGroupCache.getUser(responsibleUser.getId());
        data.setResponsibleUser(responsibleUser);
      }
      final UserSelectPanel responsibleUserSelectPanel = new UserSelectPanel(fs.newChildId(), new PropertyModel<PFUserDO>(data,
          "responsibleUser"), parentPage, "responsibleUserId");
      fs.add(responsibleUserSelectPanel);
      responsibleUserSelectPanel.init();
    }
    gridBuilder.newSplitPanel(GridSize.COL50);
    {
      // Priority drop down box:
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("priority"));
      final LabelValueChoiceRenderer<Priority> priorityChoiceRenderer = new LabelValueChoiceRenderer<Priority>(fs, Priority.values());
      final DropDownChoice<Priority> priorityChoice = new DropDownChoice<Priority>(fs.getDropDownChoiceId(), new PropertyModel<Priority>(
          data, "priority"), priorityChoiceRenderer.getValues(), priorityChoiceRenderer);
      priorityChoice.setNullValid(true);
      fs.add(priorityChoice);
    }
    {
      // Max hours:
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("task.maxHours"));
      final MinMaxNumberField<Integer> maxNumberField = new MinMaxNumberField<Integer>(InputPanel.WICKET_ID, new PropertyModel<Integer>(
          data, "maxHours"), 0, 9999);
      WicketUtils.setSize(maxNumberField, 6);
      fs.add(maxNumberField);
      if (isNew() == false && taskTree.hasOrderPositions(data.getId(), true) == true) {
        WicketUtils.setWarningTooltip(maxNumberField);
        WicketUtils.addTooltip(maxNumberField, getString("task.edit.maxHoursIngoredDueToAssignedOrders"));
      }
    }
    gridBuilder.newGridPanel();
    {
      // Short description:
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("shortDescription"));
      final IModel<String> model = new PropertyModel<String>(data, "shortDescription");
      fs.add(new MaxLengthTextField(InputPanel.WICKET_ID, model));
      fs.addJIRAField(model);
    }
    {
      // Reference
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("task.reference"));
      fs.add(new MaxLengthTextField(InputPanel.WICKET_ID, new PropertyModel<String>(data, "reference")));
    }

    // ///////////////////////////////
    // GANTT
    // ///////////////////////////////
    gridBuilder.newSplitPanel(GridSize.COL50, true);
    gridBuilder.newFormHeading(getString("task.gantt.settings"));
    gridBuilder.newSubSplitPanel(GridSize.COL50);
    {
      // Gantt object type:
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("gantt.objectType"));
      final LabelValueChoiceRenderer<GanttObjectType> objectTypeChoiceRenderer = new LabelValueChoiceRenderer<GanttObjectType>(fs,
          GanttObjectType.values());
      final DropDownChoice<GanttObjectType> objectTypeChoice = new DropDownChoice<GanttObjectType>(fs.getDropDownChoiceId(),
          new PropertyModel<GanttObjectType>(data, "ganttObjectType"), objectTypeChoiceRenderer.getValues(), objectTypeChoiceRenderer);
      objectTypeChoice.setNullValid(true);
      fs.add(objectTypeChoice);
    }
    {
      // Gantt: start date
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("gantt.startDate"));
      final DatePanel startDatePanel = new DatePanel(fs.newChildId(), new PropertyModel<Date>(data, "startDate"), DatePanelSettings.get()
          .withTargetType(java.sql.Date.class).withSelectProperty("startDate"));
      fs.add(startDatePanel);
    }
    {
      // Gantt: end date
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("gantt.endDate"));
      final DatePanel endDatePanel = new DatePanel(fs.newChildId(), new PropertyModel<Date>(data, "endDate"), DatePanelSettings.get()
          .withTargetType(java.sql.Date.class).withSelectProperty("endDate"));
      fs.add(endDatePanel);
      dependentFormComponents[1] = endDatePanel;
    }

    gridBuilder.newSubSplitPanel(GridSize.COL50);
    {
      // Progress
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("task.progress")).setUnit("%");
      final MinMaxNumberField<Integer> progressField = new MinMaxNumberField<Integer>(InputPanel.WICKET_ID, new PropertyModel<Integer>(
          data, "progress"), 0, 100) {
        @SuppressWarnings({ "unchecked", "rawtypes"})
        @Override
        public IConverter getConverter(final Class type)
        {
          return new IntegerPercentConverter(0);
        }
      };
      WicketUtils.setSize(progressField, 3);
      fs.add(progressField);
    }
    {
      // Gantt: duration
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("gantt.duration")).setNoLabelFor();
      final MinMaxNumberField<BigDecimal> durationField = new MinMaxNumberField<BigDecimal>(InputPanel.WICKET_ID,
          new PropertyModel<BigDecimal>(data, "duration"), BigDecimal.ZERO, TaskEditForm.MAX_DURATION_DAYS);
      WicketUtils.setSize(durationField, 6);
      fs.add(durationField);
      dependentFormComponents[0] = durationField;
    }
    {
      // Gantt: predecessor offset
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("gantt.predecessorOffset")).setUnit(getString("days"));
      final MinMaxNumberField<Integer> ganttPredecessorField = new MinMaxNumberField<Integer>(InputPanel.WICKET_ID,
          new PropertyModel<Integer>(data, "ganttPredecessorOffset"), Integer.MIN_VALUE, Integer.MAX_VALUE);
      WicketUtils.setSize(ganttPredecessorField, 6);
      fs.add(ganttPredecessorField);
    }
    gridBuilder.newSubSplitPanel(GridSize.COL100);
    {
      // Gantt relation type:
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("gantt.relationType"));
      final LabelValueChoiceRenderer<GanttRelationType> relationTypeChoiceRenderer = new LabelValueChoiceRenderer<GanttRelationType>(fs,
          GanttRelationType.values());
      final DropDownChoice<GanttRelationType> relationTypeChoice = new DropDownChoice<GanttRelationType>(fs.getDropDownChoiceId(),
          new PropertyModel<GanttRelationType>(data, "ganttRelationType"), relationTypeChoiceRenderer.getValues(),
          relationTypeChoiceRenderer);
      relationTypeChoice.setNullValid(true);
      fs.add(relationTypeChoice);
    }
    {
      // Gantt: predecessor
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("gantt.predecessor"));
      final TaskSelectPanel ganttPredecessorSelectPanel = new TaskSelectPanel(fs.newChildId(), new PropertyModel<TaskDO>(data,
          "ganttPredecessor"), parentPage, "ganttPredecessorId");
      fs.add(ganttPredecessorSelectPanel);
      ganttPredecessorSelectPanel.setShowFavorites(true);
      ganttPredecessorSelectPanel.init();
    }

    // ///////////////////////////////
    // FINANCE ADMINISTRATION
    // ///////////////////////////////
    gridBuilder.newSplitPanel(GridSize.COL50);
    gridBuilder.newFormHeading(getString("financeAdministration"));

    final boolean hasKost2AndTimesheetBookingAccess = ((TaskDao) getBaseDao()).hasAccessForKost2AndTimesheetBookingStatus(
        PFUserContext.getUser(), data);
    {
      // Cost 2 settings
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("fibu.kost2"));
      final ProjektDO projekt = taskTree.getProjekt(data.getId());
      if (projekt != null) {
        final DivTextPanel projektKostLabel = new DivTextPanel(fs.newChildId(), projekt.getKost() + ".*");
        WicketUtils.addTooltip(projektKostLabel.getLabel(), new Model<String>() {
          @Override
          public String getObject()
          {
            final List<Kost2DO> kost2DOs = taskTree.getKost2List(projekt, data, data.getKost2BlackWhiteItems(), data.isKost2IsBlackList());
            final String[] kost2s = TaskListPage.getKost2s(kost2DOs);
            if (kost2s == null || kost2s.length == 0) {
              return " - (-)";
            }
            return " - " + StringHelper.listToString("<br/>", kost2s);
          };
        });
        fs.add(projektKostLabel);
      }
      final PropertyModel<String> model = new PropertyModel<String>(data, "kost2BlackWhiteList");
      kost2BlackWhiteTextField = new MaxLengthTextField(InputPanel.WICKET_ID, model);
      WicketUtils.setSize(kost2BlackWhiteTextField, 10);
      fs.add(kost2BlackWhiteTextField);
      final LabelValueChoiceRenderer<Boolean> kost2listTypeChoiceRenderer = new LabelValueChoiceRenderer<Boolean>() //
          .addValue(Boolean.FALSE, getString("task.kost2list.whiteList")) //
          .addValue(Boolean.TRUE, getString("task.kost2list.blackList"));
      final DropDownChoice<Boolean> kost2listTypeChoice = new DropDownChoice<Boolean>(fs.getDropDownChoiceId(), new PropertyModel<Boolean>(
          data, "kost2IsBlackList"), kost2listTypeChoiceRenderer.getValues(), kost2listTypeChoiceRenderer);
      kost2listTypeChoice.setNullValid(false);
      fs.add(kost2listTypeChoice);
      if (hasKost2AndTimesheetBookingAccess == false) {
        kost2listTypeChoice.setEnabled(false);
        kost2BlackWhiteTextField.setEnabled(false);
      }
      final Kost2SelectPanel kost2SelectPanel = new Kost2SelectPanel(fs.newChildId(), new PropertyModel<Kost2DO>(this, "kost2Id"),
          parentPage, "kost2Id") {
        @Override
        protected void beforeSelectPage(final PageParameters parameters)
        {
          super.beforeSelectPage(parameters);
          if (projekt != null) {
            parameters.add(Kost2ListPage.PARAMETER_KEY_STORE_FILTER, false);
            parameters.add(Kost2ListPage.PARAMETER_KEY_SEARCH_STRING, "nummer:" + projekt.getKost() + ".*");
          }
        }
      };
      fs.add(kost2SelectPanel);
      kost2SelectPanel.init();
    }
    {
      // Protection until
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("task.protectTimesheetsUntil"));
      final DatePanel protectTimesheetsUntilPanel = new DatePanel(fs.newChildId(), new PropertyModel<Date>(data, "protectTimesheetsUntil"),
          DatePanelSettings.get().withTargetType(java.sql.Date.class).withSelectProperty("protectTimesheetsUntil"));
      fs.add(protectTimesheetsUntilPanel);
      if (userGroupCache.isUserMemberOfFinanceGroup() == false) {
        protectTimesheetsUntilPanel.setEnabled(false);
      }
    }
    {
      // Time sheet booking status drop down box:
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("task.timesheetBooking"));
      final LabelValueChoiceRenderer<TimesheetBookingStatus> timesheetBookingStatusChoiceRenderer = new LabelValueChoiceRenderer<TimesheetBookingStatus>(
          fs, TimesheetBookingStatus.values());
      final DropDownChoice<TimesheetBookingStatus> timesheetBookingStatusChoice = new DropDownChoice<TimesheetBookingStatus>(
          fs.getDropDownChoiceId(), new PropertyModel<TimesheetBookingStatus>(data, "timesheetBookingStatus"),
          timesheetBookingStatusChoiceRenderer.getValues(), timesheetBookingStatusChoiceRenderer);
      timesheetBookingStatusChoice.setNullValid(false);
      fs.add(timesheetBookingStatusChoice);
      if (hasKost2AndTimesheetBookingAccess == false) {
        timesheetBookingStatusChoice.setEnabled(false);
      }
    }
    {
      // Protection of privacy:
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("task.protectionOfPrivacy"));
      final DivPanel radioGroupPanel = fs.addNewRadioBoxDiv();
      final RadioGroupPanel<Boolean> radioGroup = new RadioGroupPanel<Boolean>(radioGroupPanel.newChildId(), "protectionOfPrivacy",
          new PropertyModel<Boolean>(data, "protectionOfPrivacy"));
      radioGroupPanel.add(radioGroup);
      WicketUtils.addYesNo(radioGroup);
      fs.setLabelFor(radioGroup.getRadioGroup());
      fs.addHelpIcon(getString("task.protectionOfPrivacy.tooltip"));
    }

    gridBuilder.newGridPanel();
    {
      // Description:
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("description"));
      final IModel<String> model = new PropertyModel<String>(data, "description");
      fs.add(new MaxLengthTextArea(TextAreaPanel.WICKET_ID, model), true);
      fs.addJIRAField(model);
    }
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }
}
