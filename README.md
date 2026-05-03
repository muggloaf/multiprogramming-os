# Multiprogramming Operating System Simulation

A Java-based simulation of a multiprogramming operating system, developed in two phases. The project simulates a CPU, memory management (including paging and virtual memory), process control, and error handling through hardware-level interrupts.

## Overview

This project implements a simplified OS architecture capable of loading and executing jobs from a file-based job stream.

### Phase 1: Basic Machine Simulation
- **CPU Simulation**: Includes General Purpose Register (R), Instruction Register (IR), Instruction Pointer (IC), and a Toggle Bit (C).
- **Memory**: 100-word memory (4 bytes per word), organized into 10 blocks.
- **Supervisor Calls**: Implementation of System Interrupts (SI) for I/O and program termination.
- **Instruction Set**: `GD` (Get Data), `PD` (Print Data), `LR` (Load Register), `SR` (Store Register), `CR` (Compare Register), `BT` (Branch if True), and `H` (Halt).

### Phase 2: Advanced OS Concepts
- **Virtual Memory & Paging**: 100-word virtual address space mapped to 300-word physical memory using random frame allocation.
- **Address Translation**: Dynamic translation from virtual to physical addresses using a Page Table.
- **Error Handling**: Comprehensive handling of program errors via interrupts:
  - **Programming Interrupt (PI)**: Opcode Error (1), Operand Error (2), Page Fault (3).
  - **Timing Interrupt (TI)**: Time Limit Exceeded (2).
  - **Line Limit Errors**: Monitoring line count against job specifications.
- **Process Control Block (PCB)**: Tracks Process ID (PID), Time Limits (TTL), Line Limits (TLL), and current execution status.

## Project Structure

- `phase1/`: Contains the initial implementation of the OS simulation.
  - `phase1.java`: The core logic for Phase 1.
  - `input.txt` & `output.txt`: Sample job cards and execution results.
- `phase2/`: Contains the advanced implementation with paging and error handling.
  - `phase2.java`: The core logic for Phase 2.
  - `PCB.java`: Process Control Block data structure.
  - `inputE.txt` & `inputNE.txt`: Job cards with and without intentional errors.

## Job Card Format

Jobs are fed into the system using the following control cards:
- `$AMJ`: Marks the start of a job. Contains Job ID, Time Limit (TTL), and Line Limit (TLL).
- `$DTA`: Signals the start of data cards following the program.
- `$END`: Marks the end of a job.

## How to Run

1.  Navigate to the desired phase directory (`phase1/` or `phase2/`).
2.  Compile the Java files:
    ```bash
    javac *.java
    ```
3.  Run the simulation:
    ```bash
    java phase1  # For Phase 1
    java phase2  # For Phase 2
    ```
4.  The output will be generated in `output.txt`.

## Error Statuses Handled
- **Opcode Error**: Invalid instruction entered.
- **Operand Error**: Out-of-bounds or non-numeric memory address.
- **Time Limit Exceeded**: Program execution exceeded allotted time (TTL).
- **Line Limit Exceeded**: Program output exceeded allotted lines (TLL).
- **Out of Data Error**: Program requested data beyond what was provided.
- **Page Fault**: Managed through valid (allocation) and invalid (access error) handling.
