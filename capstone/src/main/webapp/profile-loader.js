showOrHideProfile();

/** Fetch login status and show or hide profile accordingly */
function showOrHideProfile() {
  fetch('/auth').then(response => response.text()).then((loginStatus) => {
    var authContent = document.getElementById('profile-content');
    if (loginStatus.includes('Logout')) {
      authContent.innerHTML += loginStatus;
      getStudentInfo();
    }
    else {
      authContent.innerHTML = loginStatus;
    }
  })
}

/** Fetch student information and add it to the profile */
function getStudentInfo() {
  fetch('/student-data').then(response => response.json()).then((studentInfo) => {  
    // Update profile name
    var profileTitle = document.getElementById('heading');
    profileTitle.innerHTML += studentInfo['name'];

    // Update profile club list and announcements inbox
    var clubList = document.getElementById('club-content');
    var inbox = document.getElementById('inbox');
    const clubs = studentInfo['clubs'];
    for(const key in clubs) {
      const clubName = clubs[key]['name'];
      clubList.appendChild(createClubElement(clubName));
      inbox.innerHTML += clubName + ':<br>';
      inbox.appendChild(addAnnoucements(clubs[key]['announcements']));
      inbox.innerHTML += '<br>';
    }

    // Add additional student information and allow year and major to be editable
    var personalInfo = document.getElementById('student-info');
    const newLine = '<br>';
    // Create variables for start of div tag
    const startOfDivYear = '<div id="edit-year"';
    const startOfDivMajor = '<div id="edit-major"';
    const editableDiv = 'class="edit-profile" contenteditable="true">';
    const endDiv = '</div>';

    personalInfo.innerHTML += 'Email: ' + studentInfo['email'] + newLine;
    personalInfo.innerHTML += 'Grad Year: ' + startOfDivYear + editableDiv + studentInfo['gradYear'] + endDiv + newLine;
    personalInfo.innerHTML += 'Major: ' + startOfDivMajor + editableDiv + studentInfo['major'] + endDiv + newLine;
  });
}

/** Create ul and li elements for each club's announcements */
function addAnnoucements(announcements) {
  var inboxList = document.createElement('ul');

  for(const announcement in announcements){
    var liElement = document.createElement('li');
    liElement.innerText = announcements[announcement];
    inboxList.appendChild(liElement);
  }
  return inboxList;
}

/** Create an <li> element containing club name and leave button */
function createClubElement(text) {
  var liElement = document.createElement('li');

  // Create leave button and set value to its respective club
  liElement.innerHTML += text
    + '  <button name="club" value="'
    + text
    + '" formmethod="POST">Leave</button>';
  return liElement;
}

/** Store edited content from profile page */
function saveProfileChanges() {
  const newYear = document.getElementById("edit-year").innerHTML;
  const newMajor = document.getElementById("edit-major").innerHTML;

  var updateProfile = document.getElementById("update-profile");

  updateProfile.innerHTML = '<input type="hidden" name="new-year" value="' + newYear + '">';
  updateProfile.innerHTML += '<input type="hidden" name="new-major" value="' + newMajor + '">';

  document.forms['edit-profile'].submit();
}
