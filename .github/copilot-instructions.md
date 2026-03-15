# Copilot Instructions for unseen

## Objective

Before doing any repo-specific work, read relevant code context first. The central system for gameplay AI reasoning is A* pathfinding, so pathfinding and enemy-AI tasks must prioritize A* files before proposing changes.

## Prompt Routing

Classify each prompt into one of these types before responding.

1. Repo implementation/change prompt

- Example: "fix enemy chase", "refactor map logic", "add feature"
- Requirement: perform pre-read workflow and provide a Context Read Summary before coding.

2. Repo explanation prompt

- Example: "how does this file work", "what does this symbol do"
- Requirement: read only the target file plus direct dependencies/callers before answering.
- If the topic touches pathfinding/AI, include the A\* priority bundle first.

3. General non-repo prompt

- Example: language concept questions not about this workspace
- Requirement: answer directly; do not force a repository scan.

## Mandatory Pre-Read Workflow (Repo Prompts)

For any repo-specific prompt (types 1 and 2), do this before implementation guidance:

1. Identify task scope and impacted area.
2. Read files relevant to the request.
3. For pathfinding/AI requests, deep-read the A\* priority bundle first.
4. Emit a short Context Read Summary:

- Files read
- What is understood
- Assumptions or unknowns

5. Only after that, propose edits or make changes.

Do not skip the Context Read Summary for repo prompts.

## A\* Priority Bundle (Read First for Pathfinding/AI)

- src/unseen/ai/AStar.java
- src/unseen/ai/Node.java
- src/unseen/ai/Pathfinder.java
- src/unseen/ai/PathValidator.java
- src/unseen/entities/Enemy.java
- src/unseen/entities/HunterEnemy.java
- src/unseen/entities/PatrolEnemy.java
- src/unseen/map/Map.java
- src/unseen/ai/LineOfSight.java

## Relevance Rules

- Do not read unrelated folders by default (for example assets/ui) unless the prompt touches them.
- Explanation prompts should stay local: target file, direct dependencies, and immediate callers.
- For broad architectural questions, expand gradually and explicitly list the additional files read.

## Response Contract (Repo Prompts)

Start with:

- Context Read Summary
  Then provide:
- Answer or implementation plan
- Risks/assumptions when uncertainty exists

## Quality Guardrails

- Do not claim behavior without reading supporting files.
- Cite concrete files that informed the answer.
- If context is insufficient, ask a focused follow-up or read more files before concluding.
