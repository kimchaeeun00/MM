// ======== 설정 ========
const USER_ID = 1; 
const API_BASE = "http://localhost:8080/api/expense";

// ========== 1) 지출 저장 ==========
document.getElementById("addExpense").addEventListener("click", () => {
    const date = document.getElementById("expDate").value;
    const amount = document.getElementById("expAmount").value;
    const category = document.getElementById("expCategory").value;
    const memo = document.getElementById("expMemo").value;

    if (!date || !amount || !category) {
        alert("날짜, 금액, 카테고리를 모두 입력해주세요.");
        return;
    }

    const body = {
        category: category,
        amount: parseInt(amount),
        spendDate: date,
        memo: memo
    };

    fetch(`${API_BASE}/${USER_ID}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body)
    })
    .then(res => res.json())
    .then(data => {
        alert("지출이 저장되었습니다!");
        loadExpenses();   // 저장 후 새로고침
    });
});

// ========== 2) 월 변경 시 조회 ==========
document.getElementById("expMonth").addEventListener("change", () => {
    loadExpenses();
});

// ========== 3) 월별 지출 불러오기 ==========
function loadExpenses() {
    const monthInput = document.getElementById("expMonth").value;
    if (!monthInput) return;

    const year = parseInt(monthInput.substring(0, 4));
    const month = parseInt(monthInput.substring(5, 7));

    fetch(`${API_BASE}/${USER_ID}?year=${year}&month=${month}`)
        .then(res => res.json())
        .then(list => renderTable(list));
}

// ========== 4) 테이블 렌더링 ==========
function renderTable(list) {
    const tbody = document.getElementById("expTable");
    tbody.innerHTML = ""; 

    list.forEach(item => {
        const tr = document.createElement("tr");

        tr.innerHTML = `
            <td>${item.spendDate}</td>
            <td>${item.category}</td>
            <td>${item.amount.toLocaleString()}</td>
            <td>${item.memo ?? ""}</td>
            <td>
                <button onclick="deleteExpense(${item.id})" 
                    style="border:none;background:red;color:white;padding:4px 8px;border-radius:6px;cursor:pointer">
                    X
                </button>
            </td>
        `;

        tbody.appendChild(tr);
    });
}

// ========== 5) 삭제 ==========
function deleteExpense(id) {
    if (!confirm("삭제할까요?")) return;

    fetch(`${API_BASE}/${id}`, { method: "DELETE" })
        .then(() => {
            loadExpenses();  // reload
        });
}
