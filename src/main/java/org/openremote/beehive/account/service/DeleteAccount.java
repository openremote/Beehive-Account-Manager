/*
 * OpenRemote, the Home of the Digital Home.
 * Copyright 2008-2015, OpenRemote Inc.
 *
 * See the contributors.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.beehive.account.service;

import org.openremote.model.persistence.jpa.RelationalUser;
import org.openremote.model.persistence.jpa.beehive.BeehiveController;
import org.openremote.model.persistence.jpa.beehive.BeehiveUser;
import org.openremote.model.persistence.jpa.beehive.MinimalBeehiveUserRole;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DELETE;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.List;


/**
 * TODO
 *
 * @author Juha Lindfors
 */

@Path ("/users/{username}")

public class DeleteAccount
{

  // Instance Fields ------------------------------------------------------------------------------

  /**
   * Security context associated with the incoming HTTP request provided by the host HTTP servlet
   * service.
   */
  @Context private SecurityContext security;

  @Context private HttpServletRequest request;

  @Context private ServletContext webapp;

  /**
   * Inject the username value from this resource path's URI template.
   */
  @NotNull @PathParam ("username")
  private String username;


  // REST API Implementation ----------------------------------------------------------------------

  @DELETE @Produces (MediaType.TEXT_PLAIN)

  public Response delete() throws Exception
  {
    try
    {
      CreateAccount.Schema schema = CreateAccount.Schema.resolveDBSchema(webapp);

      String entityName = "User";

      if (schema == CreateAccount.Schema.LEGACY_BEEHIVE)
      {
        entityName = "BeehiveUser";
      }

      EntityManager entityManager = getEntityManager();

      List results = entityManager
          .createQuery("SELECT u FROM " + entityName + " u WHERE u.username = :name")
          .setParameter("name", username)
          .getResultList();

      if (results.isEmpty())
      {
        // TODO : align with other exception types

        throw new NotFoundException("Username was not found.");
      }

      if (schema == CreateAccount.Schema.LEGACY_BEEHIVE)
      {
        // Delete all joins to roles
        RelationalUser user = (RelationalUser) results.get(0);
        List<MinimalBeehiveUserRole> roleJoins = entityManager.createNamedQuery("findRolesForUser")
                .setParameter("userId", user.getId())
                .getResultList();
        for (MinimalBeehiveUserRole roleJoin : roleJoins)
        {
          entityManager.remove(roleJoin);
        }
        List<BeehiveController> controllers = entityManager.createNamedQuery("findControllersForAccount")
                .setParameter("account", ((BeehiveUser)user).getAccount())
                .getResultList();
        for (BeehiveController controller : controllers)
        {
          entityManager.remove(controller);
        }
      }


      entityManager.remove(results.get(0));
    }

    catch (PersistenceException exception)
    {
      throw new HttpInternalError(exception.getMessage());
    }

    return Response.ok().build();
  }



  // Private Instance Methods ---------------------------------------------------------------------

  private EntityManager getEntityManager()
  {
    return (EntityManager)request.getAttribute(AccountManager.ENTITY_MANAGER_LOOKUP);
  }
}

