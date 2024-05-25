document.addEventListener('DOMContentLoaded', () => {
  // Sample JSON data
  const data = [
    {
      "document_id": "15897",
      "title1": "Cell Text",
      "title2": "Cell Text",
      "title3": "Cell Text",
      "title4": "Cell Text"
    },
    {
      "document_id": "15897",
      "title1": "Cell Text",
      "title2": "Cell Text",
      "title3": "Cell Text",
      "title4": "Cell Text"
    },
    {
      "document_id": "15897",
      "title1": "Cell Text",
      "title2": "Cell Text",
      "title3": "Cell Text",
      "title4": "Cell Text"
    },
    {
      "document_id": "15897",
      "title1": "Cell Text",
      "title2": "Cell Text",
      "title3": "Cell Text",
      "title4": "Cell Text"
    },
    {
      "document_id": "15897",
      "title1": "Cell Text",
      "title2": "Cell Text",
      "title3": "Cell Text",
      "title4": "Cell Text"
    },
    {
      "document_id": "15897",
      "title1": "Cell Text",
      "title2": "Cell Text",
      "title3": "Cell Text",
      "title4": "Cell Text"
    },
  ];

  // Function to populate the table
  function populateTable(data) {
    const tableBody = document.querySelector("#documentTable tbody");

    data.forEach(item => {
      const row = document.createElement("tr");

      row.innerHTML = `
        <td>
            <input type="checkbox">
        </td>
        <td><a href="#">${item.document_id}</a></td>
        <td>${item.title1}</td>
        <td>${item.title2}</td>
        <td>${item.title3}</td>
        <td>${item.title4}</td>
        <td style="text-align: right;width: 120px"><img src="../assets/icons/download-dark.svg" /></td>
      `;

      tableBody.appendChild(row);
    });
  }

  // Call the function to populate the table with JSON data
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
