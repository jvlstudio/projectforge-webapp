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

import org.apache.log4j.Logger;
import org.apache.wicket.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.fibu.ProjektDO;
import org.projectforge.fibu.ProjektDao;
import org.projectforge.fibu.kost.Kost2DO;
import org.projectforge.fibu.kost.Kost2Dao;
import org.projectforge.reporting.Kost2Art;
import org.projectforge.web.wicket.AbstractBasePage;
import org.projectforge.web.wicket.AbstractEditPage;
import org.projectforge.web.wicket.EditPage;


@EditPage(defaultReturnPage = ProjektListPage.class)
public class ProjektEditPage extends AbstractEditPage<ProjektDO, ProjektEditForm, ProjektDao> implements ISelectCallerPage
{
  private static final long serialVersionUID = 8763884579951937296L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ProjektEditPage.class);

  @SpringBean(name = "kost2Dao")
  private Kost2Dao kost2Dao;

  @SpringBean(name = "projektDao")
  private ProjektDao projektDao;

  public ProjektEditPage(PageParameters parameters)
  {
    super(parameters, "fibu.projekt");
    init();
  }

  @Override
  protected ProjektDao getBaseDao()
  {
    return projektDao;
  }

  @Override
  protected ProjektEditForm newEditForm(AbstractEditPage< ? , ? , ? > parentPage, ProjektDO data)
  {
    return new ProjektEditForm(this, data);
  }

  @Override
  public AbstractBasePage afterSaveOrUpdate()
  {
    if (getData() != null && getData().getId() != null) {
      for (Kost2Art art : form.kost2Arts) {
        if (art.isExistsAlready() == false && art.isSelected() == true) {
          Kost2DO kost2 = new Kost2DO();
          kost2Dao.setProjekt(kost2, getData().getId());
          kost2Dao.setKost2Art(kost2, art.getId());
          kost2Dao.save(kost2);
        }
      }
    }
    return null;
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }

  public void cancelSelection(String property)
  {
    // Do nothing.
  }

  public void select(String property, Object selectedValue)
  {
    if ("kundeId".equals(property) == true) {
      projektDao.setKunde(getData(), (Integer) selectedValue);
    } else if ("taskId".equals(property) == true) {
      projektDao.setTask(getData(), (Integer) selectedValue);
    } else if ("projektManagerGroupId".equals(property) == true) {
      projektDao.setProjektManagerGroup(getData(), (Integer) selectedValue);
    } else {
      log.error("Property '" + property + "' not supported for selection.");
    }
  }

  public void unselect(String property)
  {
    if ("kundeId".equals(property) == true) {
      getData().setKunde(null);
    } else if ("taskId".equals(property) == true) {
      getData().setTask(null);
    } else if ("projektManagerGroupId".equals(property) == true) {
      getData().setProjektManagerGroup(null);
    } else {
      log.error("Property '" + property + "' not supported for unselection.");
    }
  }
}
