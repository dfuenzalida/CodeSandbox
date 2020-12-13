var tasks = {};

function show(id) {
  document.getElementById(id).style.display = "";
}

function hide(id) {
  document.getElementById(id).style.display = "none";
}

function valueOf(id) {
  return document.getElementById(id).value;
}

function updateById(id, html) {
  document.getElementById(id).innerHTML = html;
}

function showTaskDetail() {
  hide('taskCreateForm');
  show('taskDetails');
}

function showCreateForm() {
  hide('taskDetails');
  show('taskCreateForm');
}

function getTasks() {
  fetch('/api/tasks')
  .then(response => response.json())
  .then(data => {
    tasks = data;
    var html = '';
    data.map(task => html += renderTask(task));
    updateById('taskListContainer', html);
    // console.log(JSON.stringify(data));
  });
}

// Given a task object, return HTML markup of a task

function renderTask(task) {
  return '<a href="#" onClick="taskDetail(' + task.id + ')" ' +
         ' class="list-group-item list-group-item-action">' +
		 '<div class="d-flex w-100 justify-content-between">' +
		 '<h5 class="mb-1">' + (task.name || '<i>no name</i>') + '</h5>' +
		 '<small>' + task.state + '</small>' +
		 '</div><p class="mb-1">' + task.id + '</p></a>';
}

function taskDetail(taskId) {
	showTaskDetail();
	var task = tasks.find(t => t.id === taskId);
	var html = '<table class="table table-bordered table-striped table-condensed table-hover"><tbody>';

	Object.keys(task).map( k => {
	  var v = task[k];
	  html += '<tr><th scope="row">' + k + '</th><td>';
	  html += (k === "code" || k === "stdout" || k === "stderr") ? ('<pre>' + v + '</pre>') : v;
	  html += '</td></tr>';
	});

	html += '</tbody></table>';
    updateById('taskDetailsTable', html);
}

async function postData(url = '', data = {}) {
  const response = await fetch(url, {
    method: 'POST',
    headers: {'Content-Type': 'application/json'},
    body: JSON.stringify(data)
  });
  return response.json();
}

function submitForm() {
    var taskData = {lang: 'groovy'};
    taskData['name'] = valueOf('scriptName');
    taskData['code'] = valueOf('scriptCode');
    postData('/api/tasks', taskData).then( response => {
        updateById('alert', 'Task #' + response.id + ' created');
        show('alert');
        setTimeout(() => hide('alert'), 2000);
	    console.log(JSON.stringify(response));
	    document.getElementsByTagName('form')[0].reset();
    });
}

function init() {
  showCreateForm();
  getTasks();
  setInterval(getTasks, 3000);
}

init();