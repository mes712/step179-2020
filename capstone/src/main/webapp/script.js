// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

/** Add top navigation bar to the HTML */
const topNavBar = 
    '<div class="top-navigation">'
    + '<a href="profile.html">My Profile</a>'
    + '<a href="index.html">Explore</a>'
    + '</div>';
document.write(topNavBar);

/** Load 'About Us' Club info tab. Default page displayed when user first enters club page. */
function getClubInfo() {
  var params = new URLSearchParams(window.location.search);
  fetch('/clubs?name=' + params.get('name')).then(response => response.json()).then((clubInfo) => {
    document.getElementById('club-name').innerHTML = clubInfo['name'];
    document.getElementById('description').innerHTML = clubInfo['description'];
    
    var officerList = document.getElementById('officers');
    var officers = clubInfo['officers'];
    officerList.innerHTML = 'Officers:';
    officerList.innerHTML += '<ul>';
    for (const officer of officers) {
      officerList.innerHTML += '<li>' + officer + '</li>';
    }
    officerList.innerHTML += '</ul>'

    document.getElementById('members').innerHTML = '# of Members: ' + clubInfo['members'].length;
    document.getElementById('website').innerHTML = 'Website: ' + clubInfo['website'];
  });
}

/** Accesses and displays club announcement data from servlet. */
async function loadAnnouncements () {
  var params = new URLSearchParams(window.location.search);
  const query = '/announcements?name=' + params.get('name');
  const response = await fetch(query);
  const json = await response.json();
  const template = document.querySelector('#announcement-element');
  for (var announcement of json) {
    template.content.querySelector('li').innerHTML = announcement;
    var clone = document.importNode(template.content, true);
    document.getElementById('announcements-display').appendChild(clone);
  }
}

/** Displays a certain tab for a club, by first checking for a GET parameter 
    that specifies which tab to load, then if that doesn't exist, loads a default tab.
    This should be used at the initial load for the about-us.html page and to redirect
    back to a specific tab. Overloaded with showTab(tabName), which displays the given tab. 
 */
function showTab() {
  const params = new URLSearchParams(window.location.search);
  const tabToLoad = params.get('tab');
  const defaultTab = '#about-us'
  if (tabToLoad) {
    showTab('#' + tabToLoad);
  } else {
    showTab(defaultTab);
  }
}

/** Displays club info tab, depending on which tab is passed in. Overloaded with showTab(), where
    this one should be called with a specific tab to load.
*/
function showTab(tabName) {
  var template = document.querySelector(tabName);
  const node = document.importNode(template.content, true);
  document.getElementById('tab').innerHTML = '';
  document.getElementById('tab').appendChild(node);

  if (tabName == '#about-us') {
    getClubInfo();
  } else if (tabName == '#announcements') {
    loadAnnouncements();
  }
}

/** Adds student to members list for current club */
function joinClub() {
}