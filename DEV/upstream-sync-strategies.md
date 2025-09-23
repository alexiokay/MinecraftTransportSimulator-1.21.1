# Upstream Sync Strategies for 1.21.1 Port

This document outlines various strategies for syncing changes from the upstream DonBruce64/MinecraftTransportSimulator repository into our 1.21.1 NeoForge port.

## The Challenge

Our 1.21.1 port has a different structure than the upstream repository:
- **Upstream**: Uses `mcinterfaceforge1201/`, `mcinterfaceforge1192/`, etc.
- **Our port**: Uses `neoforge/` for the 1.21.1 interface
- **Common ground**: Both have `mccore/` with identical structure

## Strategy 1: Manual Cherry-Pick (Current Method)

**What we're doing now:**
```bash
# Check each commit manually
git show <commit-hash> --name-only
# Apply changes by hand to matching files
# Create our own commits
```

**Pros:**
- ✅ Full control over what gets applied
- ✅ Can fix bugs in upstream commits
- ✅ Clean commit history
- ✅ No "X commits behind" status

**Cons:**
- ❌ Time-consuming
- ❌ Error-prone
- ❌ Easy to miss commits

## Strategy 2: Automatic Cherry-Pick with Path Filtering

**Implementation:**
```bash
# Cherry-pick but exclude interface files we don't have
git cherry-pick --no-commit <commit-hash>
git reset HEAD -- mcinterfaceforge*
git add .
git commit -m "Cherry-pick: <original-message>"
```

**Pros:**
- ✅ Much faster than manual
- ✅ Preserves original commit messages
- ✅ Can batch process multiple commits

**Cons:**
- ❌ May apply incomplete changes (if commit spans mccore + interface)
- ❌ Merge conflicts need manual resolution

## Strategy 3: Scripted Batch Application

**Implementation:**
```bash
#!/bin/bash
for commit in $(git log --oneline HEAD..upstream/master --format="%H"); do
    # Check if commit only touches files we have
    files=$(git show --name-only $commit)
    mccore_only=$(echo "$files" | grep -v "mcinterfaceforge" | grep -v "^$")

    if [[ -n "$mccore_only" ]]; then
        echo "Auto-applying: $commit"
        git cherry-pick --no-commit $commit

        # Remove any interface files we don't have
        git reset HEAD -- mcinterfaceforge* 2>/dev/null || true

        if git diff --cached --quiet; then
            echo "No applicable changes, skipping"
            git reset --hard
        else
            git commit --reuse-message=$commit
        fi
    else
        echo "Skipping: $commit (interface-only changes)"
    fi
done
```

**Pros:**
- ✅ Fully automated
- ✅ Intelligent filtering
- ✅ Batch processing

**Cons:**
- ❌ Complex script maintenance
- ❌ May skip important changes that span both mccore and interface

## Strategy 4: Merge with Conflict Resolution Strategy

**Implementation:**
```bash
# Merge but resolve conflicts by keeping our structure
git merge -X ours upstream/master
# Manually resolve conflicts for mccore files only
# Ignore interface file conflicts
```

**Pros:**
- ✅ One-shot operation
- ✅ Git tracks the merge relationship
- ✅ Easy to see what we're behind by

**Cons:**
- ❌ Shows "merged from upstream" in history
- ❌ Complex conflict resolution
- ❌ All-or-nothing approach

## Strategy 5: Patch-Based Application

**Implementation:**
```bash
# Generate patches for mccore-only changes
git format-patch --stdout HEAD..upstream/master -- mccore/ > upstream-mccore.patch
# Apply patches selectively
git apply --check upstream-mccore.patch
git apply upstream-mccore.patch
```

**Pros:**
- ✅ File-system level filtering
- ✅ Easy to review before applying
- ✅ Standard patch format

**Cons:**
- ❌ Doesn't handle renames well
- ❌ Line-by-line conflicts

## Strategy 6: Subtree Merge for mccore/

**Implementation:**
```bash
# Add upstream as subtree for mccore only
git subtree pull --prefix=mccore upstream master --squash
```

**Pros:**
- ✅ Automated mccore sync
- ✅ Preserves history
- ✅ Git handles merging

**Cons:**
- ❌ Only works for mccore directory
- ❌ Doesn't handle interface adaptations
- ❌ Complex initial setup

## Recommended Hybrid Approach

**For maximum efficiency:**

1. **Automated filtering script** for clearly mccore-only commits
2. **Manual cherry-pick** for commits that need interface adaptation
3. **Batch commit creation** to maintain clean history

**Implementation:**
```bash
# Step 1: Auto-apply safe commits
./scripts/auto-cherry-pick-mccore.sh

# Step 2: Review remaining commits manually
git log --oneline HEAD..upstream/master

# Step 3: Manually adapt interface-dependent changes
```

## What We Applied So Far

### ✅ Successfully Applied (Manual Method):
1. **f2d178e88** - climbSpeed VMable feature
2. **91931339c** - Camera fix for multiple cameras
3. **bce14543d** - WORLD_ATTACHED particle spawning (partial)

### ⚠️ Pending Application:
- 7 more commits to review and apply
- Mix of mccore and interface changes
- Some may need adaptation for neoforge structure

## Best Practices

1. **Always check file paths** before applying commits
2. **Test compilation** after each batch of changes
3. **Create descriptive commit messages** that reference upstream commits
4. **Maintain clean history** - avoid merge commits showing "behind upstream"
5. **Document adaptations** when interface changes need manual conversion

## Future Automation Ideas

- **GitHub Action** to automatically detect applicable upstream commits
- **Weekly sync reports** showing new upstream changes
- **Automated testing** of cherry-picked commits
- **Integration with issue tracking** for upstream feature requests