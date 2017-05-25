/*
 * Copyright 2005-2017 Dozer Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dozer.vo.interfacerecursion;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Christoph Goldner 
 */
public class UserGroupPrimeImpl implements UserGroupPrime {

  private String name;

  private List<UserPrime> users = new ArrayList<UserPrime>();

  public String getName() {
    return name;
  }

  public void setName(String aName) {
    name = aName;
  }

  public List<UserPrime> getUsers() {
    return users;
  }

  public void setUsers(List<UserPrime> aUsers) {
    users = aUsers;
  }

  public void addUser(UserPrime aUser) {
    users.add(aUser);
    aUser.setUserGroup(this);
  }

}
