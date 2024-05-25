document.addEventListener("DOMContentLoaded", function() {
  const data = [
      { id: 14567, samples: "CoH-1, CoH-2", group: "unknown", site: "CoH", immunotherapy: "True", who_grade: 4, fovs: "3 NA, CoH_C001_R1C1, CoH_C002_R2C2" },
      { id: 14568, samples: "CoH-1", group: "known", site: "CoH", immunotherapy: "False", who_grade: 3, fovs: "2 NA, CoH_C003_R1C1" },
      { id: 14569, samples: "CoH-1, CoH-2, CoH-3", group: "unknown", site: "CoH", immunotherapy: "True", who_grade: 2, fovs: "1 NA, CoH_C004_R1C1, CoH_C005_R2C2, CoH_C006_R3C3" },
      { id: 14570, samples: "CoH-1, CoH-2", group: "unknown", site: "CoH", immunotherapy: "True", who_grade: 4, fovs: "3 NA, CoH_C001_R1C1, CoH_C002_R2C2" },
      { id: 14571, samples: "CoH-1", group: "known", site: "CoH", immunotherapy: "False", who_grade: 3, fovs: "2 NA, CoH_C003_R1C1" },
      { id: 14572, samples: "CoH-1, CoH-2, CoH-3", group: "unknown", site: "CoH", immunotherapy: "True", who_grade: 2, fovs: "1 NA, CoH_C004_R1C1, CoH_C005_R2C2, CoH_C006_R3C3" },
      { id: 14573, samples: "CoH-1, CoH-2", group: "unknown", site: "CoH", immunotherapy: "True", who_grade: 4, fovs: "3 NA, CoH_C001_R1C1, CoH_C002_R2C2" },
      { id: 14574, samples: "CoH-1", group: "known", site: "CoH", immunotherapy: "False", who_grade: 3, fovs: "2 NA, CoH_C003_R1C1" },
      { id: 14575, samples: "CoH-1, CoH-2, CoH-3", group: "unknown", site: "CoH", immunotherapy: "True", who_grade: 2, fovs: "1 NA, CoH_C004_R1C1, CoH_C005_R2C2, CoH_C006_R3C3" },
      { id: 14576, samples: "CoH-1, CoH-2", group: "unknown", site: "CoH", immunotherapy: "True", who_grade: 4, fovs: "3 NA, CoH_C001_R1C1, CoH_C002_R2C2" },
      { id: 14577, samples: "CoH-1", group: "known", site: "CoH", immunotherapy: "False", who_grade: 3, fovs: "2 NA, CoH_C003_R1C1" },
      { id: 14578, samples: "CoH-1, CoH-2", group: "unknown", site: "CoH", immunotherapy: "True", who_grade: 2, fovs: "1 NA, CoH_C004_R1C1, CoH_C005_R2C2, CoH_C006_R3C3" }
  ];

  const tableBody = document.getElementById("table-body");

  data.forEach(item => {
      const row = document.createElement("tr");
      
      row.innerHTML = `
          <td>${item.id}</td>
          <td>2 <a href="#">${item.samples}</a></td>
          <td>${item.group}</td>
          <td>${item.site}</td>
          <td>${item.immunotherapy}</td>
          <td>${item.who_grade}</td>
          <td>${item.fovs}</td>
      `;
      tableBody.appendChild(row);
  });


});


function openTab(event, tabName) {
  var i, tabcontent, tablinks;

  tabcontent = document.getElementsByClassName("tab-content");
  for (i = 0; i < tabcontent.length; i++) {
      tabcontent[i].style.display = "none";
  }

  tablinks = document.getElementsByClassName("tab");
  for (i = 0; i < tablinks.length; i++) {
      tablinks[i].className = tablinks[i].className.replace(" active", "");
  }

  document.getElementById(tabName).style.display = "block";
  event.currentTarget.className += " active";
}

// Open the first tab by default
document.addEventListener("DOMContentLoaded", function() {
  document.querySelector(".tab").click();
});
