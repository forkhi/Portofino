/*
* Copyright (C) 2005-2011 ManyDesigns srl.  All rights reserved.
* http://www.manydesigns.com/
*
* Unless you have purchased a commercial license agreement from ManyDesigns srl,
* the following license terms apply:
*
* This program is free software; you can redistribute it and/or modify
* it under the terms of the GNU General Public License version 3 as published by
* the Free Software Foundation.
*
* There are special exceptions to the terms and conditions of the GPL
* as it is applied to this software. View the full text of the
* exception in file OPEN-SOURCE-LICENSE.txt in the directory of this
* software distribution.
*
* This program is distributed WITHOUT ANY WARRANTY; and without the
* implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
* See the GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program; if not, see http://www.gnu.org/licenses/gpl.txt
* or write to:
* Free Software Foundation, Inc.,
* 59 Temple Place - Suite 330,
* Boston, MA  02111-1307  USA
*
*/

package com.manydesigns.portofino.shiro;

import com.manydesigns.portofino.pages.Permissions;
import com.manydesigns.portofino.security.AccessLevel;
import org.apache.shiro.authz.Permission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Paolo Predonzani     - paolo.predonzani@manydesigns.com
 * @author Angelo Lupo          - angelo.lupo@manydesigns.com
 * @author Giampiero Granatella - giampiero.granatella@manydesigns.com
 * @author Alessio Stalla       - alessio.stalla@manydesigns.com
 *
 * Permission associated to a Subject which holds the subject's groups.
 */
public class GroupPermission implements Permission {
    public static final String copyright =
            "Copyright (c) 2005-2012, ManyDesigns srl";

    protected final Collection<String> groups;
    private static final Logger logger = LoggerFactory.getLogger(GroupPermission.class);

    public GroupPermission(Collection<String> groups) {
        this.groups = groups;
    }

    public boolean implies(Permission p) {
        if(p instanceof GroupPermission) {
            GroupPermission gp = (GroupPermission) p;
            return gp.groups.containsAll(groups) && groups.containsAll(gp.groups);
        } else if(p instanceof PagePermission) {
            PagePermission pp = (PagePermission) p;
            return hasPermissions
                    (pp.getCalculatedPermissions(), groups, pp.getAccessLevel(), pp.getPermissions());
        }
        return false;
    }

    public static boolean hasPermissions
            (Permissions configuration, Collection<String> groups, AccessLevel level, String... permissions) {
        boolean hasLevel = level == null;
        boolean hasPermissions = true;
        Map<String, Boolean> permMap = new HashMap<String, Boolean>(permissions.length);
        for(String groupId : groups) {
            AccessLevel actualLevel = configuration.getActualLevels().get(groupId);
            if(actualLevel == AccessLevel.DENY) {
                return false;
            } else if(!hasLevel &&
                      actualLevel != null &&
                      actualLevel.isGreaterThanOrEqual(level)) {
                hasLevel = true;
            }

            Set<String> perms = configuration.getActualPermissions().get(groupId);
            if(perms != null) {
                for(String permission : permissions) {
                    if(perms.contains(permission)) {
                        permMap.put(permission, true);
                    }
                }
            }
        }

        for(String permission : permissions) {
            hasPermissions &= permMap.containsKey(permission);
        }

        hasPermissions = hasLevel && hasPermissions;
        if(!hasPermissions) {
            logger.debug("User does not have permissions. User's groups: {}", groups);
        }
        return hasPermissions;
    }
}