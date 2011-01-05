/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2011 Kai Reinhard (k.reinhard@me.com)
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

package org.projectforge.web.fibu;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.wicket.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.calendar.DayHolder;
import org.projectforge.common.StringHelper;
import org.projectforge.fibu.EingangsrechnungDO;
import org.projectforge.fibu.EingangsrechnungDao;
import org.projectforge.fibu.EingangsrechnungsPositionDO;
import org.projectforge.web.wicket.AbstractEditPage;
import org.projectforge.web.wicket.EditPage;

@EditPage(defaultReturnPage = EingangsrechnungListPage.class)
public class EingangsrechnungEditPage extends AbstractEditPage<EingangsrechnungDO, EingangsrechnungEditForm, EingangsrechnungDao> implements
    ISelectCallerPage
{
  private static final long serialVersionUID = 6847624027377867591L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(EingangsrechnungEditPage.class);

  @SpringBean(name = "eingangsrechnungDao")
  private EingangsrechnungDao eingangsrechnungDao;

  public EingangsrechnungEditPage(PageParameters parameters)
  {
    super(parameters, "fibu.eingangsrechnung");
    init();
    getData().recalculate(); // Muss immer gemacht werden, damit das Zahlungsziel in Tagen berechnet wird.
  }

  @Override
  protected EingangsrechnungDao getBaseDao()
  {
    return eingangsrechnungDao;
  }

  @Override
  protected EingangsrechnungEditForm newEditForm(AbstractEditPage< ? , ? , ? > parentPage, EingangsrechnungDO data)
  {
    return new EingangsrechnungEditForm(this, data);
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }

  /**
   * Clones the data positions and reset the date and target date etc.
   */
  protected void cloneRechnung()
  {
    log.info("Clone of invoice chosen: " + getData());
    final EingangsrechnungDO rechnung = getData();
    rechnung.setId(null);
    final int zahlungsZielInTagen = rechnung.getZahlungsZielInTagen();
    final DayHolder day = new DayHolder();
    rechnung.setDatum(day.getSQLDate());
    day.add(Calendar.DAY_OF_MONTH, zahlungsZielInTagen);
    rechnung.setFaelligkeit(day.getSQLDate());
    final List<EingangsrechnungsPositionDO> positionen = getData().getPositionen();
    if (positionen != null) {
      rechnung.setPositionen(new ArrayList<EingangsrechnungsPositionDO>());
      for (final EingangsrechnungsPositionDO origPosition : positionen) {
        final EingangsrechnungsPositionDO position = (EingangsrechnungsPositionDO) origPosition.newClone();
        rechnung.addPosition(position);
      }
    }
    form.refresh();
    form.cloneButtonPanel.setVisible(false);
  }

  public void cancelSelection(String property)
  {
    // Do nothing.
  }

  public void select(String property, Object selectedValue)
  {
    if (StringHelper.isIn(property, "datum", "faelligkeit", "bezahlDatum") == true) {
      final Date date = (Date) selectedValue;
      final java.sql.Date sqlDate = new java.sql.Date(date.getTime());
      if ("datum".equals(property) == true) {
        getData().setDatum(sqlDate);
        form.datumPanel.markModelAsChanged();
      } else if ("faelligkeit".equals(property) == true) {
        getData().setFaelligkeit(sqlDate);
        form.faelligkeitPanel.markModelAsChanged();
      } else if ("bezahlDatum".equals(property) == true) {
        getData().setBezahlDatum(sqlDate);
        form.bezahlDatumPanel.markModelAsChanged();
      }
      getData().recalculate();
    } else {
      log.error("Property '" + property + "' not supported for selection.");
    }
  }

  public void unselect(String property)
  {
    log.error("Property '" + property + "' not supported for selection.");
  }
}
