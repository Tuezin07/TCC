// ======================== 
// VARIÁVEIS GLOBAIS
// ========================
let produtos = [];
let cpfInformado = null;
let pagamentoPollingHandle = null;

// ELEMENTOS
const telaInicial = document.getElementById('tela-inicial');
const telaLeitura = document.getElementById('tela-leitura');
const telaPagamento = document.getElementById('tela-pagamento');
const modalCpf = document.getElementById('modal-cpf');
const modalPagamento = document.getElementById('modal-pagamento');
const telaFinalizacao = document.getElementById('tela-finalizacao');
const modalErro = document.getElementById('modal-erro'); // novo modal
const erroMensagem = document.getElementById('erro-mensagem'); // span/p/div dentro do modal

const btnIniciar = document.getElementById('iniciar');
const btnFinalizar = document.getElementById('finalizar-compra');
const btnVoltar = document.getElementById('voltar');
const btnVoltarPagamento = document.getElementById('voltar-pagamento');

const btnSemCpf = document.getElementById('btn-sem-cpf');
const btnComCpf = document.getElementById('btn-com-cpf');
const cpfInputContainer = document.getElementById('cpf-input-container');
const cpfDisplay = document.getElementById('cpf-display');
const numpadBtns = document.querySelectorAll('.numpad-btn');
const btnClear = document.getElementById('btn-clear');
const btnConfirm = document.getElementById('btn-confirm');

const codigoBarras = document.getElementById('codigo-barras');
const produtosLidos = document.getElementById('produtos-lidos');
const valorTotal = document.getElementById('valor-total');
const contadorProdutos = document.getElementById('contador-produtos');
const dataHora = document.getElementById('data-hora');
const valorPix = document.getElementById('valor-pix');
const qrCodeImg = document.querySelector('#qr-code img');

// ========================
// INICIALIZAÇÃO
// ========================
document.addEventListener('DOMContentLoaded', () => {
    atualizarDataHora();
    setInterval(atualizarDataHora, 60000);
});

function atualizarDataHora() {
    const agora = new Date();
    const options = {
        weekday: 'long',
        day: 'numeric',
        month: 'long',
        hour: '2-digit',
        minute: '2-digit'
    };
    dataHora.textContent = agora.toLocaleDateString('pt-BR', options);
}

// ========================
// FLUXO DE TELAS
// ========================
btnIniciar.addEventListener('click', () => {
    telaInicial.style.display = 'none';
    modalCpf.style.display = 'flex';
});

btnSemCpf.addEventListener('click', () => {
    modalCpf.style.display = 'none';
    telaLeitura.style.display = 'flex';
    codigoBarras.focus();
});

btnComCpf.addEventListener('click', () => {
    cpfInputContainer.style.display = 'block';
    btnComCpf.style.display = 'none';
    btnSemCpf.style.display = 'none';
});

// ========================
// CPF NUMPAD
// ========================
numpadBtns.forEach(btn => {
    if (btn.id !== 'btn-clear' && btn.id !== 'btn-confirm') {
        btn.addEventListener('click', () => {
            const digit = btn.textContent;
            let currentCpf = cpfDisplay.textContent;
            const index = currentCpf.indexOf('_');

            if (index !== -1) {
                let newCpf = currentCpf.split('');
                newCpf[index] = digit;
                cpfDisplay.textContent = newCpf.join('');

                if (!cpfDisplay.textContent.includes('_')) {
                    cpfInformado = cpfDisplay.textContent;
                }
            }
        });
    }
});

btnClear.addEventListener('click', () => {
    let newCpf = cpfDisplay.textContent.split('');
    for (let i = newCpf.length - 1; i >= 0; i--) {
        if (newCpf[i] !== '_' && newCpf[i] !== '.' && newCpf[i] !== '-') {
            newCpf[i] = '_';
            break;
        }
    }
    cpfDisplay.textContent = newCpf.join('');
});

btnConfirm.addEventListener('click', () => {
    if (!cpfDisplay.textContent.includes('_')) {
        cpfInformado = cpfDisplay.textContent;
        modalCpf.style.display = 'none';
        telaLeitura.style.display = 'flex';
        codigoBarras.focus();
    } else {
        mostrarErro('Informe um CPF completo ou clique em "Não, obrigado".');
    }
});

// ========================
// LEITURA DE PRODUTO
// ========================
codigoBarras.addEventListener('keypress', (e) => {
    if (e.key === 'Enter') {
        const codigo = codigoBarras.value.trim();
        if (!codigo) return;

        fetch(`/produtos/${codigo}`)
            .then(response => {
                if (!response.ok) throw new Error('Produto não encontrado');
                return response.json();
            })
            .then(produto => {
                adicionarProduto(produto);
                codigoBarras.value = '';
            })
            .catch(() => {
                mostrarErro('Produto não encontrado!');
                codigoBarras.value = '';
            });
    }
});

function adicionarProduto(produto) {
    produtos.push(produto);
    renderizarProdutos();
    calcularTotal();
}

function renderizarProdutos() {
    produtosLidos.innerHTML = '';
    contadorProdutos.textContent =
        `${produtos.length} ${produtos.length === 1 ? 'item' : 'itens'}`;
    
    produtos.forEach(produto => {
        const div = document.createElement('div');
        div.className = 'produto-item';
        div.innerHTML = `
            <span class="produto-nome">${produto.nome}</span>
            <span class="produto-preco">R$ ${produto.preco.toFixed(2)}</span>
        `;
        produtosLidos.appendChild(div);
    });
}

function calcularTotal() {
    const total = produtos.reduce((sum, p) => sum + p.preco, 0);
    valorTotal.textContent = `R$ ${total.toFixed(2)}`;
    valorPix.textContent = `R$ ${total.toFixed(2)}`;
}

// ========================
// FINALIZAR COMPRA
// ========================
btnFinalizar.addEventListener('click', () => {
    if (produtos.length === 0) {
        mostrarErro('Nenhum produto adicionado!');
        return;
    }
    modalPagamento.style.display = 'flex';
});

btnVoltarPagamento.addEventListener('click', () => {
    modalPagamento.style.display = 'none';
});

// ========================
// PAGAMENTO
// ========================
document.querySelectorAll('.pagamento-btn').forEach(btn => {
    btn.addEventListener('click', () => {
        const metodo = btn.getAttribute('data-tipo');

        modalPagamento.style.display = 'none';

        if (metodo === 'pix') {
            criarPagamentoEMostrarQR();
        } else {
            mostrarErro('Por enquanto somente PIX está disponível.');
        }
    });
});

btnVoltar.addEventListener('click', () => {
    telaPagamento.style.display = 'none';
    telaLeitura.style.display = 'flex';

    if (qrCodeImg) qrCodeImg.src = 'https://via.placeholder.com/250x250?text=QR+CODE';

    if (pagamentoPollingHandle) {
        clearInterval(pagamentoPollingHandle);
        pagamentoPollingHandle = null;
    }
});

// ========================
// CRIAR PAGAMENTO E MOSTRAR QR
// ========================
async function criarPagamentoEMostrarQR() {
    const total = produtos.reduce((sum, p) => sum + p.preco, 0);

    if (isNaN(total) || total <= 0) {
        mostrarErro('Valor inválido para pagamento.');
        return;
    }

    const payload = { valorTotal: total, metodoPagamento: "PIX" };

    try {
        const resp = await fetch('https://caixa-rapido-backend.onrender.com/vendas', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (!resp.ok) {
            const err = await resp.json().catch(() => ({}));
            console.error('Erro criar pagamento:', err);
            mostrarErro('Erro ao gerar pagamento. Tente novamente.');
            return;
        }

        const data = await resp.json();
        const qrBase64 = data.qrCodeBase64 || data.qrCode || null;
        const paymentId = (data.paymentId || data.payment_id || data.id || '').toString();
        const ticketUrl = data.urlPagamento || data.ticket_url || null;

        if (!qrBase64 && !ticketUrl) {
            mostrarErro('QR Code não disponível. Tente novamente.');
            return;
        }

        telaLeitura.style.display = 'none';
        telaPagamento.style.display = 'flex';

        if (qrBase64 && qrCodeImg) qrCodeImg.src = 'data:image/png;base64,' + qrBase64;
        else if (ticketUrl && qrCodeImg) qrCodeImg.src = ticketUrl;

        if (valorPix) valorPix.textContent = `R$ ${total.toFixed(2)}`;

        if (!paymentId) {
            console.warn('paymentId não retornado; polling não será iniciado.');
            return;
        }

        if (pagamentoPollingHandle) {
            clearInterval(pagamentoPollingHandle);
            pagamentoPollingHandle = null;
        }

        const checkInterval = 3000;
        pagamentoPollingHandle = setInterval(async () => {
            try {
                const statusResp = await fetch(`https://caixa-rapido-backend.onrender.com/vendas/status/${encodeURIComponent(paymentId)}`);
                if (!statusResp.ok) {
                    console.warn('Erro ao consultar status do pagamento');
                    return;
                }

                const statusJson = await statusResp.json();
                const status = (statusJson.status || '').toString().toUpperCase();

                if (status === 'APPROVED' || status === 'APROVADO') {
                    clearInterval(pagamentoPollingHandle);
                    pagamentoPollingHandle = null;

                    telaPagamento.style.display = 'none';
                    telaFinalizacao.style.display = 'flex';

                    let tempo = 10;
                    const contador = document.getElementById('contador-tempo');
                    if (contador) contador.textContent = tempo;

                    const intervalo = setInterval(() => {
                        tempo--;
                        if (contador) contador.textContent = tempo;
                        if (tempo <= 0) {
                            clearInterval(intervalo);
                            resetarSistema();
                        }
                    }, 1000);
                }
            } catch (e) {
                console.error('Erro no polling de status', e);
            }
        }, checkInterval);

    } catch (error) {
        console.error('Erro ao criar pagamento:', error);
        mostrarErro('Erro ao processar pagamento. Tente novamente.');
    }
}

// ========================
// RESETAR SISTEMA
// ========================
function resetarSistema() {
    produtos = [];
    cpfInformado = null;

    if (pagamentoPollingHandle) {
        clearInterval(pagamentoPollingHandle);
        pagamentoPollingHandle = null;
    }

    telaFinalizacao.style.display = 'none';
    telaInicial.style.display = 'flex';

    produtosLidos.innerHTML = '';
    valorTotal.textContent = 'R$ 0,00';
    contadorProdutos.textContent = '0 itens';

    if (qrCodeImg) qrCodeImg.src = 'https://via.placeholder.com/250x250?text=QR+CODE';
}

// ========================
// MODAL DE ERRO
// ========================
function mostrarErro(msg) {
    if (!modalErro || !erroMensagem) {
        alert(msg); // fallback
        return;
    }

    erroMensagem.textContent = msg;
    modalErro.style.display = 'flex';

    setTimeout(() => {
        modalErro.style.display = 'none';
    }, 3000);
}
