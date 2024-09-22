Dicionario de linguagem ubíqua do dominio
=========================================

## Atores

- Cliente: Consumidor final do estabelecimento, é quem realiza a compra das refeições. Podem ser:
  - Cliente identificado: possui seus dados (CPF, Nome, Email) cadastrados no estabelecimento, e ao fazer uma compra permite recuperar os dados através do CPF;
  - Cliente não identificado: não possui cadastro no estabelecimento, ou não deseja utilizá-lo. Informa apenas seu Nome para uso durante o atendimento do pedido.
- Chefe da cozinha: Responsável por verificar os pedidos que chegam, acompanhar seu preparo e garantir que sejam entregues com rapidez e qualidade;
- Equipe da cozinha: Responsável pelo preparo dos alimentos
- Atendente: Interage com o cliente. É responsável por auxiliá-los a fazer o pedido no sistema e entregar o pedido após preparo.


## Entidades principais

- Cliente: Representação dos dados do "Cliente" descrito anteriormente
- Item do Cardapio: Representação de cada item disponível para venda. Podem ser dos tipos: Lanche, Acompanhamento, Bebida ou Sobremesa
- Carrinho de compras: Armazenamento temporário dos produtos selecionados pelo cliente. Ao fechar a compra o Carrinho dá origem a um novo Pedido.
- Pagamento: Informação de pagamento realizado pelo cliente. Para cada forma de pagamento existem informações específicas a serem armazenadas nos dados de Pagamento. 
Na versão inicial DINHEIRO será a única forma de pagamento suportada.


- Pedido: É o registro de cada compra solicitada pelos clientes. Estas são suas informações principais:
  - Itens que fazem parte do pedido
  - Dados de Pagamento associados
  - Acompanhamento de Status

## Principais eventos do fluxo de pedido e entrega

- Identificação do cliente

- Seleção de produtos

- Pagamento

- Validação do Pedido

- Pedido pronto

- Pedido finalizado
