<div align="center">

# AlkaKits

### Kits com níveis pagos, requisitos, vouchers e concessão automática

Kits que evoluem por nível de upgrade pago em qualquer moeda da AlkaEconomy,
com requisitos configuráveis, feedback rico por clique e vouchers físicos
resgatáveis — tudo sobre a infraestrutura do AlkaCore.

![Java](https://img.shields.io/badge/Java-21-orange)
![Minecraft](https://img.shields.io/badge/Minecraft-1.21.8-green)
![Version](https://img.shields.io/badge/Version-1.0.3-blue)
![License](https://img.shields.io/badge/License-Proprietary-red)

</div>

---

## 📋 Sobre o Projeto

O **AlkaKits** organiza os kits do servidor em categorias, cada kit com
múltiplos **níveis de upgrade** — o jogador começa no nível 1 e paga (em
qualquer moeda cadastrada na AlkaEconomy) para desbloquear o próximo,
respeitando requisitos configuráveis por nível. O resgate tem cooldown
próprio, feedback completo (som, partícula, título e fogos) por resultado de
clique, e todo o progresso é persistido de forma **write-through** — nenhum
estado de sessão para descarregar no shutdown.

Kits também podem ser concedidos automaticamente (primeiro join do jogador,
respawn) ou distribuídos como **voucher físico** resgatável com um comando,
para uso em eventos e recompensas.

## ✨ Funcionalidades Principais

- **Níveis de upgrade pagos** por kit, com custo em qualquer moeda da
  AlkaEconomy (config-driven, zero moeda hardcoded).
- **Requisitos configuráveis** por nível antes de liberar o upgrade.
- **Menu em 3 telas** (categorias → kits → preview), com bordas
  preenchidas e título/quantidade de linhas customizáveis por tela.
- **Feedback rico por resultado de clique** — som, partícula, título/legenda
  e fogos de artifício configuráveis por status (recebido, comprado,
  bloqueado, requisito não cumprido, em cooldown, saldo insuficiente,
  inventário cheio, nível máximo).
- **Vouchers físicos**: crie e distribua itens que resgatam um kit em um
  nível específico com `/kits resgatar`.
- **Concessão automática** por gatilho — primeiro join do jogador e/ou
  respawn, configurável em `kits.yml`.
- **Placeholders (PlaceholderAPI)** para expor progresso de kits em
  scoreboard, chat e outros plugins.
- Zero conexão de banco própria e zero `InventoryClickListener` próprio —
  banco e GUI vêm inteiramente do **AlkaCore**.

## 🎮 Comandos

| Comando | Descrição | Permissão |
| --- | --- | --- |
| `/kits [kit\|resgatar]` | Abre o menu de categorias, o preview direto de um kit, ou resgata o voucher na mão. | `alkakits.usar` |
| `/alkakits reload` | Recarrega config, mensagens, kits e gatilhos de concessão. | `alkakits.admin` |
| `/alkakits give <player> <kit> [nível]` | Concede um kit (em um nível específico) diretamente a um jogador online. | `alkakits.admin` |
| `/alkakits reset <player> <kit>` | Zera o progresso de um kit para o jogador. | `alkakits.admin` |
| `/alkakits setlevel <player> <kit> <nível>` | Define o nível desbloqueado de um kit para o jogador. | `alkakits.admin` |
| `/alkakits voucher give <player> <kit> <nível>` | Gera e entrega um voucher físico resgatável para o kit/nível informado. | `alkakits.admin` |

## 🔗 Integrações

- **AlkaCore** (dependência direta) — banco de dados (HikariCP/SQLite/MySQL)
  e motor de GUI (BaseGui/GuiListener); o AlkaKits não abre conexão JDBC nem
  registra listener de inventário próprio.
- **AlkaEconomy** (dependência direta) — cobra o custo de upgrade de nível em
  qualquer moeda cadastrada.
- **PlaceholderAPI** (opcional) — expansão própria com placeholders de
  progresso de kit.

## 🔧 Tecnologias Utilizadas

- **Java 21** · **Gradle** (com `shadow`)
- **Paper API 1.21.8**
- **AlkaCore** (banco + GUI) · **AlkaEconomy** (moedas)
- **Adventure/MiniMessage** para mensagens e GUI

## ⚙️ Instalação

1. Instale **AlkaCore** e **AlkaEconomy** (dependências obrigatórias).
2. Coloque `AlkaKits.jar` na pasta `plugins/` do servidor (Paper **1.21.8+**).
3. Reinicie o servidor.
4. Configure `plugins/AlkaKits/kits.yml` (kits, níveis, requisitos, custo) e
   `config.yml` (gatilhos automáticos, menus, feedback por clique).

## 🔐 Permissões

| Permissão | Descrição | Padrão |
| --- | --- | --- |
| `alkakits.usar` | Permite usar `/kits`. | `true` |
| `alkakits.admin` | Acesso administrativo completo ao AlkaKits. | `op` |

## 📝 Licença

> ⚠️ **Projeto proprietário da AlkaStudio.**
>
> Código fonte destinado exclusivamente ao uso interno da rede `Alka*`.
> Reprodução, distribuição ou uso não autorizado não são permitidos.

## 🎯 Créditos

- **Desenvolvido por**: MestreDEV — AlkaStudio
- **Parte do**: ecossistema `Alka*`

---

<div align="center">

**Desenvolvido com ❤️ pela AlkaStudio**

[![AlkaStudio](https://img.shields.io/badge/AlkaStudio-JLob0-blue)](https://github.com/JLob0)

</div>
