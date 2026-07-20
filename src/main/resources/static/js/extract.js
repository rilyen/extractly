// holds most recently extracted JSON object (the whole { Products: [...] })
let lastJSON = null;

// Fields stored in arrays for mapping elements of same type
// maps each JSON key to the form element's id.
const textField = ["Deal_ID", "Product_Name", "Product_Description",
    "Total_Product_Cost_Hours", "Estimated_Duration_to_Implement_Days",
    "General_Comments", "Task_ID"];

const Checkboxes = ["Passed_IAT", "User_Story_Created"];

const DateFields = ["Latest_Review_Date"];

// which product is currently loaded into the form.
// Ive assigned them (and im using them) as an index and treated them as sigle objects
let currentProduct = 0;


// triggered by the "Extract JSON" button
async function extract() {
    const transcript = document.getElementById('transcript').value.trim();

    if (!transcript) {
        setStatus('Paste a transcript first.');
        return;
    }

    setStatus('Calling Gemini...');

    try {
        // A Post request to /extract endpoint
        // Backend controller returns a JSON object response
        const res = await fetch('/extract', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ transcript: transcript })
        });

        // parse response body as JSON
        const data = await res.json();

        // if request fails throw to catch block below
        if (!res.ok) {
            throw new Error(data.error || 'Request failed.');
        }

        // // Check to make sure Gemini returned a candidate result.
        // if (!data.candidates || !data.candidates[0]) {
        //     throw new Error('Gemini returned no result. Try again or shorten the transcript.');
        // }
        // const raw = data.candidates[0].content.parts[0].text;
        // // log raw text to inspect formatting issues
        // console.log('Raw Gemini text:', raw);

        // // strip markdown code so the string can be parsed as valid JSON and trim whitespace
        // const clean = raw.replace(/```json|```/g, '').trim();

        // convert clean string to JS object
        lastJSON = parseGeminiResponse(data);


        // display the parsed result with 2-space indentation
        // enable action buttons
        // document.getElementById('jsonOutput').textContent = JSON.stringify(lastJSON, null, 2);

        // Pushes the values from the JSON object into the form fields. 
        // The mapping function
        // fillForm(lastJSON);

        showJSON(lastJSON);

        // fill the product dropdown and load the first product into the form
        selectProduct(lastJSON);

        setStatus('Extraction complete. Review and edit before submitting.');

    } catch (err) {
        setStatus('Error: ' + err.message);
        console.error(err);
    }
}

function parseGeminiResponse(data) {
    // Check to make sure Gemini returned a candidate result.
    if (!data.candidates || !data.candidates[0]) {
        throw new Error('Gemini returned no result. Try again or shorten the transcript.');
    }
    const candidate = data.candidates[0];
    // join ALL parts, long responses may be split across several
    const parts = (candidate.content && candidate.content.parts) || [];
    const raw = parts.map(p => p.text || '').join('');
    // log raw text and finish reason to inspect formatting issues
    console.log('Gemini finishReason:', candidate.finishReason);
    console.log('Raw Gemini text:', raw);
    // strip markdown code fences so the string can be parsed as valid JSON
    const clean = raw.replace(/```json|```/g, '').trim();
    try {
        // convert clean string to JS object
        return JSON.parse(clean);
    } catch (err) {
        // show the raw text on the page so the failure point can be inspected
        const output = document.getElementById('jsonOutput');
        if (output) {
            output.textContent = 'PARSE FAILED. Raw Gemini output below:\n\n' + raw;
        }
        if (candidate.finishReason === 'MAX_TOKENS') {
            throw new Error('Gemini hit its output token limit, the JSON is cut off. Shorten the transcript or raise maxOutputTokens.');
        }
        throw new Error('Could not parse Gemini output as JSON: ' + err.message);
    }
}

// Print extracted JSON object into #jsonOutput block on the page
function showJSON(obj) {
    const output = document.getElementById('jsonOutput');
    if (output) {
        output.textContent = JSON.stringify(obj, null, 2);
    }
}

// triggered by the "Submit File" button (still in progress)
async function extractFromVideo() {

    const fileInput = document.getElementById("videoInput");
    const file = fileInput.files[0];

    if (!file) {
        alert("Please select an mp4 file first.");
        return;
    }

    setStatus('Extracting data...');

    const formData = new FormData();
    formData.append("file", file);

    try {
        const res = await fetch("/extract-from-video", {
            method: "POST",
            body: formData,
        });

        // parse response body as JSON
        const data = await res.json();

        // if request fails throw to catch block below
        if (!res.ok) {
            throw new Error(data.error || 'Request failed.');
        }                                                             

        // Check to make sure Gemini returned a candidate result.
        if (!data.candidates || !data.candidates[0]) {
            throw new Error('Gemini returned no result. Try again or shorten the transcript.');
        }

        const raw = data.candidates[0].content.parts[0].text;
        // log raw text to inspect formatting issues
        console.log('Raw Gemini text:', raw);

        // strip markdown code so the string can be parsed as valid JSON and trim whitespace
        const clean = raw.replace(/```json|```/g, '').trim();

        // convert clean string to JS object
        lastJSON = JSON.parse(clean);

        // fillForm(lastJSON);
        showJSON(lastJSON);
        setStatus('Extraction complete. Review the JSON below.');
        // setStatus('Form has been filled. Review before submitting.');
    } catch (err) {
        console.error('Video extraction failed:', err);
        setStatus(err.message || 'Something went wrong.');
    }

}


// Function to fill the product dropdown field with each of the menttioned product name, then load the product data into the field
function selectProduct(data) {
    const products = (data && data.Products) || [];
    const productSelected = document.getElementById('productSelected');
    const table = document.getElementById('productOptions');
    if (!productSelected) return;
    productSelected.innerHTML = '';

    // add one option per product
    for (let i = 0; i < products.length; i++) {
        const option = document.createElement('option');
        option.value = i;
        option.textContent = products[i].Product_Name || ('Product ' + (i + 1));
        productSelected.appendChild(option);
    }

    // shows the products in the drop down
    if (products.length > 0) {
        table.style.display = 'block';
        currentProduct = 0;
        productSelected.value = 0;
        fillForm(products[0]);
    } else {
        table.style.display = 'none';
    }
}

// called when the user picks a different product from the dropdown
function ProductChosen() {
    saveFormToProduct(currentProduct);          // Function I wrote below that saves any changes/ edits on current product before switching to a different product
    const productSelected = document.getElementById('productSelected');
    currentProduct = parseInt(productSelected.value, 10);
    fillForm(lastJSON.Products[currentProduct]);
}


// Maps the values from the JSON object to the corresponding zohoForm fields(based on the Field Type)
function fillForm(map) {
    if (!map) return;

    // Map the text box fields
    textField.forEach(key => {
        const element = document.getElementById(key);
        // Only set the value if the element exists and the key is present in the map
        if (element && map[key] != null) {
            element.value = map[key];
        } else if (element) {
            element.value = '';
        }
    });

    // Map the checkboxes. Set the box to checked if true. Fasle or missing values leave the box unchecked.
    Checkboxes.forEach(key => {
        const element = document.getElementById(key);
        if (element) {
            element.checked = map[key] === true;
        }
    });

    // Map the date fields to dd-MMM-yyyy format
    DateFields.forEach(key => {
        const element = document.getElementById(key);
        if (element) {
            if (map[key] != null) {
                element.value = dateFormat(map[key]);
            } else {
                element.value = '';
            }
        }
    });

    // We are always setting the Service type to be "Custom Functions"(we need it for the IF and THENs)
    const Custom = document.getElementById("ServiceType");
    if (Custom) {
        Custom.value = "Custom Functions";
    }

    // Map THE dropdown fields. It is set only if the value is one of the allowed options. Otherwise, it is left as the default option
    setDropdown("Project", map.Project);
    setDropdown("DealNameAccountContact", map.Deal_Name_Account_Contact);
    setDropdown("DeliveryRate", map.Delivery_Rate);
    setDropdown("ReviewerApprover", map.Reviewer_Approver);

    // creates the IFs and THENs tables for the product
    AutomationTriggerTable(map.Automation_Triggers || []);
    StandardFunctionOutput(map.Standard_Function_Outputs || []);
}


function AutomationTriggerTable(triggers) {             // IF TABLE
    const body = document.getElementById('IfTable');
    body.innerHTML = '';
    for (let i = 0; i < triggers.length; i++) {
        addIfEntry(triggers[i]);
    }
}

// The function adds a new entry to IFs Automation trigger
// Dynamically adds a entire row of entry
function addIfEntry(entry) {
    if (!entry){ 
        entry = {};
    }
    const body = document.getElementById('IfTable');
    const row = document.createElement('tr');

    // These adds a cell column to table entry and matches the text format in zoho creator for easier zoho mapping
    const numberCell = CreateTextInput(entry.Trigger_Number);
    const appCell = CreateTextInput(entry.Name_of_Trigger_Application);
    const zohoCell = makeYesNoCell(entry.Is_the_application_a_Zoho_App);
    const eventCell = CreateTextArea(entry.Trigger_Event_Description);
    const filtersCell = CreateTextArea(entry.Filters);
    const assumeCell = CreateTextArea(entry.Trigger_Assumptions);
    const hoursCell = CreateTextInput(entry.Estimated_Hours);
    const deleteEntry = DeleteCellButton();

    // Dynamically adds a entire row of entry
    row.appendChild(numberCell);
    row.appendChild(appCell);
    row.appendChild(zohoCell);
    row.appendChild(eventCell);
    row.appendChild(filtersCell);
    row.appendChild(assumeCell);
    row.appendChild(hoursCell);
    row.appendChild(deleteEntry);

    body.appendChild(row);
}


function StandardFunctionOutput(outputs) {                           // THEN TABLE
    const body = document.getElementById('ThenTable');
    body.innerHTML = '';
    for (let i = 0; i < outputs.length; i++) {
        addThenEntry(outputs[i]);
    }
}

// The function adds a new entry to Standard Function Outputs (THENs)
// Dynamically adds a entire row of entry
function addThenEntry(entry) {
    if (!entry){ 
        entry = {};
    }
    const body = document.getElementById('ThenTable');
    const row = document.createElement('tr');

    const triggerCell = CreateTextInput(entry.Select_Trigger);
    const servicesCell = CreateTextInput(entry.Standard_Services);
    const inclCell = CreateTextArea(entry.Inclusions);
    const exclCell = CreateTextArea(entry.Exclusions);
    const descCell = CreateTextArea(entry.Detailed_Description);
    const hoursCell = CreateTextInput(entry.Estimated_Hours);
    const deleteEntry = DeleteCellButton();
    row.appendChild(triggerCell);
    row.appendChild(servicesCell);
    row.appendChild(inclCell);
    row.appendChild(exclCell);
    row.appendChild(descCell);
    row.appendChild(hoursCell);
    row.appendChild(deleteEntry);
    body.appendChild(row);
}


// Functions that dynamically create(replicate) and set html text command formats and assign values based on IFs and THENs in the JSON
// table data with a text input inside
function CreateTextInput(entry) {
    const td = document.createElement('td');
    const input = document.createElement('input');
    input.type = 'text';
    input.className = 'form-control';
    if (entry != null) input.value = entry;
    td.appendChild(input);
    return td;
}

// table data with a textarea inside
function CreateTextArea(entry) {
    const td = document.createElement('td');
    const area = document.createElement('textarea');
    area.className = 'form-control';
    area.rows = 2;
    if (entry != null) area.value = entry;
    td.appendChild(area);
    return td;
}

// table data with a dropdown that will display Yes/No dropdown
function makeYesNoCell(entry) {
    const td = document.createElement('td');
    const select = document.createElement('select');
    select.className = 'form-select';
    select.innerHTML =
        '<option value="">-Select-</option>' +
        '<option value="Yes">Yes</option>' +
        '<option value="No">No</option>';
    if (entry != null) select.value = entry;
    td.appendChild(select);
    return td;
}

// delete button that removes entire row
function DeleteCellButton() {
    const td = document.createElement('td');
    const button = document.createElement('button');
    button.type = 'button';
    button.className = 'btn btn-sm btn-outline-danger';
    button.textContent = 'x';
    button.onclick = function () {
        const row = button.parentElement.parentElement;
        row.remove();
    };
    td.appendChild(button);
    return td;
}


// Function that saves any changes made to the web pages fields so that when we flip to different product the edits still remain
// makes the edits directly into lastJSON
// its run everytime before we switch products
function saveFormToProduct(index) {
    if (!lastJSON || !lastJSON.Products || !lastJSON.Products[index]) return;
    const product = lastJSON.Products[index];           // product object to update and store below values

    // the text fields
    textField.forEach(key => {
        const element = document.getElementById(key);
        if (element) {
            product[key] = element.value !== '' ? element.value : null;
        }
    });

    // the drop down
    Checkboxes.forEach(key => {
        const element = document.getElementById(key);
        if (element) {
            product[key] = element.checked;
        }
    });

    // the date fields
    DateFields.forEach(key => {
        const element = document.getElementById(key);
        if (element) {
            product[key] = element.value !== '' ? element.value : null;
        }
    });

    // the drop downs
    product.Project = readValue("Project");
    product.Deal_Name_Account_Contact = readValue("DealNameAccountContact");
    product.Delivery_Rate = readValue("DeliveryRate");
    product.Reviewer_Approver = readValue("ReviewerApprover");
    product.Service_Types = ["Custom Functions"];

    // the IF/THEN tables
    product.Automation_Triggers = saveIfEntries();
    product.Standard_Function_Outputs = saveThenEntries();
}

// save all IF rows back into an array of objects and turns then into trigger objects
function saveIfEntries() {
    const rows = document.getElementById('IfTable').children;
    const Triggerlist = [];
    for (let i = 0; i < rows.length; i++) {
        const cells = rows[i].children;                 // read each cell's value into fixed column
        const trigger = {
            Trigger_Number: ColumnValue(cells[0]),
            Name_of_Trigger_Application: ColumnValue(cells[1]),
            Is_the_application_a_Zoho_App: ColumnValue(cells[2]),
            Trigger_Event_Description: ColumnValue(cells[3]),
            Filters: ColumnValue(cells[4]),
            Trigger_Assumptions: ColumnValue(cells[5]),
            Estimated_Hours: ColumnValue(cells[6])
        };
        Triggerlist.push(trigger);
    }
    return Triggerlist;        // All the triggers are stored on the product
}

// read all THEN rows back into an array of objects
function saveThenEntries() {
    const rows = document.getElementById('ThenTable').children;
    const Triggerlist = [];
    for (let i = 0; i < rows.length; i++) {
        const entry = rows[i].children;
        const output = {
            Select_Trigger: ColumnValue(entry[0]),
            Standard_Services: ColumnValue(entry[1]),
            Inclusions: ColumnValue(entry[2]),
            Exclusions: ColumnValue(entry[3]),
            Detailed_Description: ColumnValue(entry[4]),
            Estimated_Hours: ColumnValue(entry[5])
        };
        Triggerlist.push(output);
    }
    return Triggerlist;
}

// get the value of the input inside the table column
function ColumnValue(cell) {
    const field = cell.children[0];        // the input or select or the textarea
    if (!field) return null;
    return field.value !== '' ? field.value : null;
}


// dropdown is set only if the value is not empty
function setDropdown(id, field) {
    const element = document.getElementById(id);
    if (!element || field == null) return;
    element.value = field;
}

// read a dropdown's value, or null if empty
function readValue(id) {
    const element = document.getElementById(id);
    if (element && element.value !== '') {
        return element.value;
    }
    return null;
}


// Convert date format from YYYY-MM-DD to dd-MMM-YYYY (example. 2026-03-15 to 15-Mar-2026)
function dateFormat(set_date) {

    // parse the date string to a Date object and check if it worked
    const date = new Date(set_date);
    if (isNaN(date)) {
        return set_date;
    }
    // set the date to specific format using Intl.DateTimeFormat
    const parts = new Intl.DateTimeFormat("en-GB", {          // en-GB is british format dd-mm-yyyy
        day: "2-digit",
        month: "short",
        year: "numeric",
        timeZone: "UTC"
    }).formatToParts(date);
    const day = parts.find(key => key.type === "day").value;
    const month = parts.find(key => key.type === "month").value;
    const year = parts.find(key => key.type === "year").value;
    return `${day}-${month}-${year}`;
}


// triggered by the "Copy" button
function copyJSON() {
    if (!lastJSON) return;
    navigator.clipboard.writeText(JSON.stringify(lastJSON, null, 2))
        .then(() => setStatus('Copied to clipboard!'))
        .catch(() => setStatus('Could not copy.'));
}

// update status message shown to the user
function setStatus(msg) {
    document.getElementById('status').textContent = msg;
}

// logout
async function logout() {
    try {
        await fetch('/logout', { method: 'POST' });
        window.location.href = '/login.html';
    } catch (err) {
        console.error('Logout failed:', err);
        setStatus('Logout failed. Try again.');
    }
}

// Submit to Zoho: save current form edits into the JSON first, then send everything
async function sendJsonToZoho() {
    if (!lastJSON) {
        setStatus('Enter the Transcript.');
        return;
    }

    // save edits from the form on screen before sending
    saveFormToProduct(currentProduct);
    showJSON(lastJSON);

    const res = await fetch('/send-to-service', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(lastJSON)
    });
    const result = await res.json();
    setStatus(result.code === 3000 ? 'Message sent successfully' : 'Fail: ' + JSON.stringify(result));
}