document.addEventListener('DOMContentLoaded', () => {

  const data = [
      {
	  "title": "Master Feature Table",
	  "name": "20240810_master_feature_table_na_removed_metadata.rds",
	  "size": "35M",
	  "format": "rds"
      },
      {
	  "title": "Cell Table (immune)",
	  "name": "cell_table_immune_thresholded.parquet",
	  "size": "1G",
	  "format": "parquet"
      },
      {
	  "title": "Cell Table (tumor)",
	  "name": "cell_table_tumor_thresholded.parquet",
	  "size": "1G",
	  "format": "parquet"
      }
  ];

  function populateTable(data) {
    const tableBody = document.querySelector("#documentTable tbody");

    data.forEach(item => {
      const row = document.createElement("tr");

      row.innerHTML = `
        <td>
            <input type="checkbox">
        </td>
        <td>${item.title}</td>
        <td>${item.name}</td>
        <td>${item.size}</td>
        <td>${item.format}</td>
        <td style="text-align: right;width: 120px">
<a href="https://storage.googleapis.com/pici-bruce-vitessce-public/other/${item.name}" download="${item.name}"><img src="../assets/icons/download-dark.svg" />
</a></td>
      `;

      tableBody.appendChild(row);
    });
  }

  populateTable(data);

  // Function to handle "select all" checkbox
  document.getElementById("selectAll").addEventListener("change", function() {
    const checkboxes = document.querySelectorAll("#documentTable tbody input[type='checkbox']");
    checkboxes.forEach(checkbox => checkbox.checked = this.checked);
  });

  // Tab functionality (if needed)
  const tabs = document.querySelectorAll('.tab');
  tabs.forEach(tab => {
    tab.addEventListener('click', function() {
      tabs.forEach(t => t.classList.remove('active'));
      this.classList.add('active');
      // Implement tab switching logic here if needed
    });
  });
});
